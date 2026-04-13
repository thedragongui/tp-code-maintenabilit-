package com.epsi.rpg.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.security.reset-token=test-reset-token")
class GameControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate db;

    private MockMvc mvc;

    private static final String PID = "test-player-1";
    private static final String PID2 = "test-player-2";
    private static final String RESET_TOKEN = "test-reset-token";

    @BeforeEach
    void setup() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(post("/api/v1/hero/reset").header("X-ADMIN-TOKEN", RESET_TOKEN))
                .andExpect(status().isOk());
    }

    private void initPlayer(String id) throws Exception {
        mvc.perform(get("/api/v1/hero/" + id))
                .andExpect(status().isOk());
    }

    private String json(Object... kvPairs) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < kvPairs.length; i += 2) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(kvPairs[i]).append("\":");
            Object v = kvPairs[i + 1];
            if (v instanceof Number) {
                sb.append(v);
            } else {
                sb.append("\"").append(v).append("\"");
            }
        }
        return sb.append("}").toString();
    }

    @Test
    void getHero_newPlayer_createsWithDefaults() throws Exception {
        mvc.perform(get("/api/v1/hero/" + PID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.name").value("Invite"))
                .andExpect(jsonPath("$.player.level").value(1))
                .andExpect(jsonPath("$.player.hp").value(100))
                .andExpect(jsonPath("$.player.maxHp").value(100))
                .andExpect(jsonPath("$.enemy").exists());
    }

    @Test
    void getHero_invalidPlayerId_returnsBadRequest() throws Exception {
        mvc.perform(get("/api/v1/hero/bad id"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHero_existingPlayer_returnsCurrentState() throws Exception {
        initPlayer(PID);
        db.update("UPDATE players SET name = 'Hero' WHERE id = ?", PID);

        mvc.perform(get("/api/v1/hero/" + PID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.name").value("Hero"));
    }

    @Test
    void getAllPlayers_returnsAllRegisteredPlayers() throws Exception {
        initPlayer(PID);
        initPlayer(PID2);

        mvc.perform(get("/api/v1/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    void setupHero_withMode_updatesNameClassAndMode() throws Exception {
        initPlayer(PID);

        mvc.perform(post("/api/v1/hero/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID, "name", "Aragorn", "class", "Guerrier", "mode", "COOP")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.name").value("Aragorn"))
                .andExpect(jsonPath("$.player.mode").value("COOP"));
    }

    @Test
    void setupHero_withBlankMode_updatesOnlyNameAndClass() throws Exception {
        initPlayer(PID);

        mvc.perform(post("/api/v1/hero/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID, "name", "Legolas", "class", "Archer", "mode", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.name").value("Legolas"));
    }

    @Test
    void setupHero_withNullMode_updatesOnlyNameAndClass() throws Exception {
        initPlayer(PID);

        mvc.perform(post("/api/v1/hero/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerId\":\"" + PID + "\",\"name\":\"Gimli\",\"class\":\"Nain\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.name").value("Gimli"));
    }

    @Test
    void getLogs_returnsLogList() throws Exception {
        initPlayer(PID);

        mvc.perform(get("/api/v1/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void attack_soloMode_enemyAlive_playerReceivesCounterDamage() throws Exception {
        initPlayer(PID);
        db.update("UPDATE solo_enemies SET hp = 9999 WHERE player_id = ?", PID);

        mvc.perform(post("/api/v1/battle/attack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID, "mode", "SOLO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.hp").value(lessThan(100)));
    }

    @Test
    void attack_soloMode_enemyKilled_spawnsNextLevel() throws Exception {
        initPlayer(PID);
        db.update("UPDATE solo_enemies SET hp = 1, level = 1 WHERE player_id = ?", PID);

        mvc.perform(post("/api/v1/battle/attack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID, "mode", "SOLO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enemy.level").value(2));
    }

    @Test
    void attack_soloMode_level10Multiple_spawnsBossEnemy() throws Exception {
        initPlayer(PID);
        db.update("UPDATE solo_enemies SET hp = 1, level = 9 WHERE player_id = ?", PID);

        mvc.perform(post("/api/v1/battle/attack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID, "mode", "SOLO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enemy.name").value(containsString("INSTANCE BOSS")));
    }

    @Test
    void attack_deadPlayer_returnsStateWithoutCombat() throws Exception {
        initPlayer(PID);
        db.update("UPDATE players SET hp = 0 WHERE id = ?", PID);
        int enemyHpBefore = db.queryForObject("SELECT hp FROM solo_enemies WHERE player_id = ?", Integer.class, PID);

        mvc.perform(post("/api/v1/battle/attack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID, "mode", "SOLO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enemy.hp").value(enemyHpBefore));
    }

    @Test
    void attack_coopMode_bossAlive_reducesBossHpAndCounters() throws Exception {
        initPlayer(PID);
        db.update("UPDATE players SET mode = 'COOP' WHERE id = ?", PID);
        db.update("UPDATE world_boss SET hp = 2000 WHERE id = 1");

        mvc.perform(post("/api/v1/battle/attack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID, "mode", "COOP")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enemy.hp").value(lessThan(2000)));
    }

    @Test
    void attack_coopMode_bossKilled_respawnsStronger() throws Exception {
        initPlayer(PID);
        db.update("UPDATE players SET mode = 'COOP' WHERE id = ?", PID);
        db.update("UPDATE world_boss SET hp = 1, max_hp = 2000, level = 10 WHERE id = 1");

        mvc.perform(post("/api/v1/battle/attack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID, "mode", "COOP")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enemy.hp").value(greaterThanOrEqualTo(2000)));
    }

    @Test
    void attack_pvpMode_reducesTargetHp() throws Exception {
        initPlayer(PID);
        initPlayer(PID2);

        int hpBefore = db.queryForObject("SELECT hp FROM players WHERE id = ?", Integer.class, PID2);

        mvc.perform(post("/api/v1/battle/attack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID, "mode", "PVP", "targetId", PID2)))
                .andExpect(status().isOk());

        int hpAfter = db.queryForObject("SELECT hp FROM players WHERE id = ?", Integer.class, PID2);
        assert hpAfter < hpBefore;
    }

    @Test
    void attack_pvpMode_withoutTarget_returnsBadRequest() throws Exception {
        initPlayer(PID);
        initPlayer(PID2);

        mvc.perform(post("/api/v1/battle/attack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID, "mode", "PVP")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void attack_pvpMode_selfTarget_returnsBadRequest() throws Exception {
        initPlayer(PID);

        mvc.perform(post("/api/v1/battle/attack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID, "mode", "PVP", "targetId", PID)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void autoTick_autoLvlZero_returnsStateImmediately() throws Exception {
        initPlayer(PID);

        mvc.perform(post("/api/v1/battle/auto-tick")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player").exists());
    }

    @Test
    void autoTick_autoLvlPositive_triggersCombat() throws Exception {
        initPlayer(PID);
        db.update("UPDATE players SET auto_lvl = 2 WHERE id = ?", PID);
        db.update("UPDATE solo_enemies SET hp = 9999 WHERE player_id = ?", PID);

        mvc.perform(post("/api/v1/battle/auto-tick")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enemy.hp").value(lessThan(9999)));
    }

    @Test
    void heal_restoresPlayerToMaxHp() throws Exception {
        initPlayer(PID);
        db.update("UPDATE players SET hp = 10 WHERE id = ?", PID);

        mvc.perform(post("/api/v1/hero/heal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.hp").value(100));
    }

    @Test
    void upgradeAuto_enoughXp_incrementsAutoLvl() throws Exception {
        initPlayer(PID);
        db.update("UPDATE players SET xp = 9999 WHERE id = ?", PID);

        mvc.perform(post("/api/v1/hero/upgrade-auto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID)))
                .andExpect(status().isOk());

        int autoLvl = db.queryForObject("SELECT auto_lvl FROM players WHERE id = ?", Integer.class, PID);
        assert autoLvl == 1;
    }

    @Test
    void upgradeAuto_notEnoughXp_returnsBadRequest() throws Exception {
        initPlayer(PID);

        mvc.perform(post("/api/v1/hero/upgrade-auto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void useItem_potion_healsToMaxHp() throws Exception {
        initPlayer(PID);
        db.update("UPDATE players SET hp = 10, inv = ? WHERE id = ?", "\uD83E\uDDEA", PID);

        mvc.perform(post("/api/v1/hero/use-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerId\":\"" + PID + "\",\"itemIndex\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.hp").value(100));
    }

    @Test
    void useItem_shield_increasesMaxHp() throws Exception {
        initPlayer(PID);
        db.update("UPDATE players SET inv = ? WHERE id = ?", "\uD83D\uDEE1\uFE0F", PID);

        mvc.perform(post("/api/v1/hero/use-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerId\":\"" + PID + "\",\"itemIndex\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.maxHp").value(150));
    }

    @Test
    void useItem_invalidIndex_returnsStateUnchanged() throws Exception {
        initPlayer(PID);

        mvc.perform(post("/api/v1/hero/use-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerId\":\"" + PID + "\",\"itemIndex\":99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player").exists());
    }

    @Test
    void useItem_negativeIndex_returnsBadRequest() throws Exception {
        initPlayer(PID);

        mvc.perform(post("/api/v1/hero/use-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerId\":\"" + PID + "\",\"itemIndex\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHero_xpSufficient_triggersLevelUp() throws Exception {
        initPlayer(PID);
        db.update("UPDATE players SET xp = 200 WHERE id = ?", PID);

        mvc.perform(get("/api/v1/hero/" + PID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.level").value(greaterThan(1)));
    }

    @Test
    void getHero_multipleLevelUps_handledWithoutRecursion() throws Exception {
        initPlayer(PID);
        db.update("UPDATE players SET xp = 9999 WHERE id = ?", PID);

        mvc.perform(get("/api/v1/hero/" + PID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.level").value(greaterThan(2)));
    }

    @Test
    void getHero_coopMode_returnsWorldBossAsEnemy() throws Exception {
        initPlayer(PID);
        db.update("UPDATE players SET mode = 'COOP' WHERE id = ?", PID);

        mvc.perform(get("/api/v1/hero/" + PID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enemy.name").value("LE MONOLITHE EPSI"));
    }

    @Test
    void getHero_missingEnemy_fallsBackToWorldBoss() throws Exception {
        initPlayer(PID);
        db.update("DELETE FROM solo_enemies WHERE player_id = ?", PID);

        mvc.perform(get("/api/v1/hero/" + PID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enemy.name").value("LE MONOLITHE EPSI"));
    }

    @Test
    void reset_withoutToken_returnsForbidden() throws Exception {
        mvc.perform(post("/api/v1/hero/reset"))
                .andExpect(status().isForbidden());
    }

    @Test
    void reset_withToken_dropsAndRecreatesAllTables() throws Exception {
        initPlayer(PID);

        mvc.perform(post("/api/v1/hero/reset").header("X-ADMIN-TOKEN", RESET_TOKEN))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/hero/" + PID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.name").value("Invite"));
    }
}
