package com.epsi.rpg.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(originPatterns = "${app.cors.allowed-origin-patterns:http://localhost:*}")
public class GameController {

    private static final Set<String> VALID_MODES = Set.of("SOLO", "COOP", "PVP");
    private static final Pattern PLAYER_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,64}$");
    private static final int MAX_LEVEL_UPS_PER_REFRESH = 500;

    private static volatile boolean isInit = false;

    private final JdbcTemplate db;
    private final String resetToken;
    private final Random random = new Random();

    public GameController(JdbcTemplate db, @Value("${app.security.reset-token:}") String resetToken) {
        this.db = db;
        this.resetToken = resetToken == null ? "" : resetToken.trim();
    }

    private synchronized void checkDb() {
        if (isInit) {
            return;
        }

        db.execute("CREATE TABLE IF NOT EXISTS players (" +
                "id VARCHAR(255) PRIMARY KEY, " +
                "name VARCHAR(100), " +
                "class VARCHAR(50), " +
                "hp INT, " +
                "max_hp INT, " +
                "level INT, " +
                "xp INT, " +
                "auto_lvl INT, " +
                "mode VARCHAR(50), " +
                "inv VARCHAR(500))");

        db.execute("CREATE TABLE IF NOT EXISTS world_boss (" +
                "id INT PRIMARY KEY, " +
                "name VARCHAR(255), " +
                "hp INT, " +
                "max_hp INT, " +
                "level INT, " +
                "sprite VARCHAR(10))");
        db.execute("MERGE INTO world_boss KEY(id) VALUES (1, 'LE MONOLITHE EPSI', 2000, 2000, 10, '\uD83D\uDC79')");

        db.execute("CREATE TABLE IF NOT EXISTS solo_enemies (" +
                "player_id VARCHAR(255) PRIMARY KEY, " +
                "name VARCHAR(255), " +
                "hp INT, " +
                "max_hp INT, " +
                "level INT, " +
                "sprite VARCHAR(10))");

        db.execute("CREATE TABLE IF NOT EXISTS game_logs (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "msg VARCHAR(500), " +
                "ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        isInit = true;
        saveLog("RPG system initialized.");
    }

    @GetMapping("/hero/{id}")
    public Map<String, Object> getHero(@PathVariable String id) {
        checkDb();
        String pid = validatePlayerId(id, "id");
        ensurePlayerExists(pid);
        return internalGetState(pid);
    }

    @GetMapping("/players")
    public List<Map<String, Object>> getAllPlayers() {
        checkDb();
        return db.queryForList("SELECT id, name, class, level, hp, max_hp FROM players");
    }

    @PostMapping("/hero/setup")
    public Map<String, Object> setupHero(@RequestBody Map<String, String> body) {
        checkDb();

        String id = requireBodyPlayerId(body, "playerId");
        ensurePlayerExists(id);

        String name = requireText(body.get("name"), "name", 1, 100);
        String type = requireText(body.get("class"), "class", 1, 50);
        String rawMode = body.get("mode");
        String mode = rawMode == null || rawMode.isBlank() ? null : normalizeMode(rawMode);

        if (mode != null) {
            db.update("UPDATE players SET name = ?, class = ?, mode = ? WHERE id = ?", name, type, mode, id);
        } else {
            db.update("UPDATE players SET name = ?, class = ? WHERE id = ?", name, type, id);
        }

        saveLog("Hero " + name + " started the quest.");
        return internalGetState(id);
    }

    @GetMapping("/logs")
    public List<Map<String, Object>> getLogs() {
        checkDb();
        List<Map<String, Object>> rows = db.queryForList("SELECT msg, ts FROM game_logs ORDER BY ts");

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("msg", row.getOrDefault("msg", row.get("MSG")));
            item.put("ts", row.getOrDefault("ts", row.get("TS")));
            result.add(item);
        }
        return result;
    }

    @PostMapping("/battle/attack")
    public Map<String, Object> attack(@RequestBody Map<String, String> body) {
        checkDb();

        String pid = requireBodyPlayerId(body, "playerId");
        ensurePlayerExists(pid);

        String mode = normalizeMode(body.get("mode"));
        String targetId = optionalPlayerId(body.get("targetId"), "targetId");

        if ("PVP".equals(mode)) {
            if (targetId == null) {
                throw badRequest("targetId is required in PVP mode.");
            }
            if (pid.equals(targetId)) {
                throw badRequest("You cannot target yourself in PVP mode.");
            }
            ensurePlayerExists(targetId);
        }

        int damage = 10 + random.nextInt(15);
        return processCombat(pid, mode, targetId, damage, true);
    }

    @PostMapping("/battle/auto-tick")
    public Map<String, Object> autoTick(@RequestBody Map<String, String> body) {
        checkDb();

        String pid = requireBodyPlayerId(body, "playerId");
        ensurePlayerExists(pid);

        Map<String, Object> player = db.queryForMap("SELECT auto_lvl, mode FROM players WHERE id = ?", pid);
        int autoLvl = ((Number) player.get("auto_lvl")).intValue();
        if (autoLvl <= 0) {
            return internalGetState(pid);
        }

        String mode = normalizeMode((String) player.get("mode"));
        return processCombat(pid, mode, null, autoLvl * 5, false);
    }

    @PostMapping("/hero/heal")
    public Map<String, Object> heal(@RequestBody Map<String, String> body) {
        checkDb();

        String pid = requireBodyPlayerId(body, "playerId");
        ensurePlayerExists(pid);

        db.update("UPDATE players SET hp = max_hp WHERE id = ?", pid);
        saveLog("Full heal for " + shortId(pid));
        return internalGetState(pid);
    }

    @PostMapping("/hero/upgrade-auto")
    public ResponseEntity<?> upgradeAuto(@RequestBody Map<String, String> body) {
        checkDb();

        String pid = requireBodyPlayerId(body, "playerId");
        ensurePlayerExists(pid);

        Map<String, Object> player = db.queryForMap("SELECT xp, auto_lvl FROM players WHERE id = ?", pid);
        int currentXp = ((Number) player.get("xp")).intValue();
        int currentAuto = ((Number) player.get("auto_lvl")).intValue();
        int cost = (int) Math.round(100 * Math.pow(1.3, currentAuto));

        if (currentXp >= cost) {
            db.update("UPDATE players SET xp = xp - ?, auto_lvl = auto_lvl + 1 WHERE id = ?", cost, pid);
            saveLog("Auto-attack upgraded to level " + (currentAuto + 1));
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.badRequest().body("Not enough XP");
    }

    @PostMapping("/hero/use-item")
    public Map<String, Object> useItem(@RequestBody Map<String, Object> body) {
        checkDb();

        String pid = requireBodyPlayerId(body, "playerId");
        ensurePlayerExists(pid);

        int idx = requireNonNegativeInt(body.get("itemIndex"), "itemIndex");
        Map<String, Object> player = db.queryForMap("SELECT inv FROM players WHERE id = ?", pid);

        String rawInventory = Objects.toString(player.get("inv"), "");
        List<String> inv = Arrays.stream(rawInventory.split(","))
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));

        if (idx >= inv.size()) {
            return internalGetState(pid);
        }

        String item = inv.get(idx);
        if (item.contains("\uD83E\uDDEA")) {
            db.update("UPDATE players SET hp = max_hp WHERE id = ?", pid);
        }
        if (item.contains("\uD83D\uDEE1\uFE0F")) {
            db.update("UPDATE players SET max_hp = max_hp + 50, hp = hp + 50 WHERE id = ?", pid);
        }

        inv.remove(idx);
        db.update("UPDATE players SET inv = ? WHERE id = ?", String.join(",", inv), pid);
        return internalGetState(pid);
    }

    private Map<String, Object> processCombat(String pid, String mode, String targetId, int dmg, boolean riposte) {
        Map<String, Object> player = db.queryForMap("SELECT name, hp, level FROM players WHERE id = ?", pid);
        String playerName = Objects.toString(player.get("name"), "Unknown");

        if (((Number) player.get("hp")).intValue() <= 0) {
            return internalGetState(pid);
        }

        if ("PVP".equals(mode)) {
            if (targetId == null || pid.equals(targetId)) {
                throw badRequest("Invalid PVP target.");
            }
            int updated = db.update("UPDATE players SET hp = GREATEST(0, hp - ?) WHERE id = ?", dmg, targetId);
            if (updated == 0) {
                throw badRequest("Target player does not exist.");
            }
            saveLog("[PVP] " + playerName + " attacks Player_" + shortId(targetId));
            return internalGetState(pid);
        }

        if ("COOP".equals(mode)) {
            Map<String, Object> boss = db.queryForMap("SELECT * FROM world_boss WHERE id = 1");
            int newBossHp = ((Number) boss.get("hp")).intValue() - dmg;
            if (newBossHp <= 0) {
                int nextBossLevel = ((Number) boss.get("level")).intValue() + 1;
                int nextBossHp = (int) (((Number) boss.get("max_hp")).intValue() * 1.25);
                db.update("UPDATE world_boss SET hp = ?, max_hp = ?, level = ? WHERE id = 1",
                        nextBossHp, nextBossHp, nextBossLevel);
                db.update("UPDATE players SET xp = xp + (level * 20) WHERE id = ?", pid);
                saveLog("World boss defeated. Difficulty increased.");
            } else {
                db.update("UPDATE world_boss SET hp = ? WHERE id = 1", newBossHp);
                if (riposte) {
                    db.update("UPDATE players SET hp = GREATEST(0, hp - 12) WHERE id = ?", pid);
                }
            }
            return internalGetState(pid);
        }

        Map<String, Object> enemy = findOrCreateSoloEnemy(pid);
        int enemyHp = ((Number) enemy.get("hp")).intValue() - dmg;
        if (enemyHp <= 0) {
            int nextEnemyLevel = ((Number) enemy.get("level")).intValue() + 1;
            db.update("UPDATE players SET xp = xp + (30 + level * 5) WHERE id = ?", pid);
            spawnSoloEnemy(pid, nextEnemyLevel);
        } else {
            db.update("UPDATE solo_enemies SET hp = ? WHERE player_id = ?", enemyHp, pid);
            if (riposte) {
                int retaliateDamage = 5 + (((Number) enemy.get("level")).intValue() / 2);
                db.update("UPDATE players SET hp = GREATEST(0, hp - ?) WHERE id = ?", retaliateDamage, pid);
            }
        }

        return internalGetState(pid);
    }

    private Map<String, Object> findOrCreateSoloEnemy(String pid) {
        try {
            return db.queryForMap("SELECT * FROM solo_enemies WHERE player_id = ?", pid);
        } catch (EmptyResultDataAccessException ex) {
            spawnSoloEnemy(pid, 1);
            return db.queryForMap("SELECT * FROM solo_enemies WHERE player_id = ?", pid);
        }
    }

    private void spawnSoloEnemy(String pid, int level) {
        String name = (level % 10 == 0) ? "INSTANCE BOSS #" + (level / 10) : "Processus Malveillant";
        String sprite = (level % 10 == 0) ? "\uD83D\uDC79" : "\uD83D\uDC7E";
        int hp = (int) (60 * Math.pow(1.15, level));

        int updated = db.update(
                "UPDATE solo_enemies SET name = ?, hp = ?, max_hp = ?, level = ?, sprite = ? WHERE player_id = ?",
                name, hp, hp, level, sprite, pid);

        if (updated == 0) {
            db.update("INSERT INTO solo_enemies (player_id, name, hp, max_hp, level, sprite) VALUES (?, ?, ?, ?, ?, ?)",
                    pid, name, hp, hp, level, sprite);
        }
    }

    private void ensurePlayerExists(String id) {
        Integer count = db.queryForObject("SELECT COUNT(*) FROM players WHERE id = ?", Integer.class, id);
        if (count == null || count == 0) {
            db.update(
                    "INSERT INTO players (id, name, class, hp, max_hp, level, xp, auto_lvl, mode, inv) " +
                            "VALUES (?, 'Invite', 'Guerrier', 100, 100, 1, 0, 0, 'SOLO', '\uD83E\uDDEA,\uD83D\uDEE1\uFE0F')",
                    id);
            db.update(
                    "INSERT INTO solo_enemies (player_id, name, hp, max_hp, level, sprite) VALUES (?, 'Bug Racine', 80, 80, 1, '\uD83D\uDC7E')",
                    id);
        }
    }

    private Map<String, Object> internalGetState(String pid) {
        Map<String, Object> player = db.queryForMap("SELECT * FROM players WHERE id = ?", pid);
        applyLevelUpsIfNeeded(pid, player);
        player = db.queryForMap("SELECT * FROM players WHERE id = ?", pid);

        String mode = normalizeMode((String) player.get("mode"));
        Map<String, Object> enemy;

        try {
            enemy = "COOP".equals(mode)
                    ? db.queryForMap("SELECT * FROM world_boss WHERE id = 1")
                    : db.queryForMap("SELECT * FROM solo_enemies WHERE player_id = ?", pid);
        } catch (Exception ex) {
            enemy = db.queryForMap("SELECT * FROM world_boss WHERE id = 1");
        }

        Map<String, Object> playerData = new HashMap<>();
        playerData.put("name", player.get("name"));
        playerData.put("hp", player.get("hp"));
        playerData.put("maxHp", player.get("max_hp"));
        playerData.put("level", player.get("level"));
        playerData.put("xp", player.get("xp"));
        playerData.put("class", player.get("class"));
        playerData.put("autoAttackLevel", player.get("auto_lvl"));
        playerData.put("mode", mode);
        playerData.put("nextLevelXp", xpThresholdForLevel(((Number) player.get("level")).intValue()));

        String rawInv = Objects.toString(player.get("inv"), "");
        List<String> items = Arrays.stream(rawInv.split(","))
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.toList());
        playerData.put("inventory", items);

        Map<String, Object> enemyData = new HashMap<>();
        enemyData.put("name", enemy.get("name"));
        enemyData.put("hp", enemy.get("hp"));
        enemyData.put("maxHp", enemy.get("max_hp"));
        enemyData.put("sprite", enemy.get("sprite"));
        enemyData.put("level", enemy.get("level"));

        Map<String, Object> result = new HashMap<>();
        result.put("player", playerData);
        result.put("enemy", enemyData);
        return result;
    }

    private void applyLevelUpsIfNeeded(String pid, Map<String, Object> player) {
        int xp = ((Number) player.get("xp")).intValue();
        int level = ((Number) player.get("level")).intValue();
        int levelsGained = 0;

        while (levelsGained < MAX_LEVEL_UPS_PER_REFRESH) {
            int threshold = xpThresholdForLevel(level);
            if (xp < threshold) {
                break;
            }
            xp -= threshold;
            level++;
            levelsGained++;
        }

        if (levelsGained <= 0) {
            return;
        }

        int hpGain = levelsGained * 30;
        db.update("UPDATE players SET level = ?, xp = ?, max_hp = max_hp + ?, hp = max_hp + ? WHERE id = ?",
                level, xp, hpGain, hpGain, pid);
        saveLog("Level up! New level: " + level);
    }

    private int xpThresholdForLevel(int level) {
        return (int) (100 * Math.pow(1.4, Math.max(0, level - 1)));
    }

    private void saveLog(String message) {
        db.update("INSERT INTO game_logs (msg) VALUES (?)", message);
    }

    @PostMapping("/hero/reset")
    public ResponseEntity<Void> reset(@RequestHeader(value = "X-ADMIN-TOKEN", required = false) String adminToken) {
        checkDb();
        requireResetToken(adminToken);

        synchronized (this) {
            isInit = false;
            db.execute("DROP TABLE IF EXISTS players");
            db.execute("DROP TABLE IF EXISTS world_boss");
            db.execute("DROP TABLE IF EXISTS solo_enemies");
            db.execute("DROP TABLE IF EXISTS game_logs");
        }

        checkDb();
        return ResponseEntity.ok().build();
    }

    private void requireResetToken(String adminToken) {
        if (resetToken.isBlank()) {
            return;
        }
        if (!Objects.equals(resetToken, adminToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid admin token.");
        }
    }

    private String requireBodyPlayerId(Map<String, ?> body, String fieldName) {
        if (body == null) {
            throw badRequest("Request body is required.");
        }
        return validatePlayerId(Objects.toString(body.get(fieldName), null), fieldName);
    }

    private String optionalPlayerId(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return validatePlayerId(value, fieldName);
    }

    private String validatePlayerId(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw badRequest(fieldName + " is required.");
        }
        String normalized = value.trim();
        if (!PLAYER_ID_PATTERN.matcher(normalized).matches()) {
            throw badRequest(fieldName + " must match [A-Za-z0-9_-]{3,64}.");
        }
        return normalized;
    }

    private String normalizeMode(String value) {
        if (value == null || value.isBlank()) {
            throw badRequest("mode is required.");
        }
        String mode = value.trim().toUpperCase(Locale.ROOT);
        if (!VALID_MODES.contains(mode)) {
            throw badRequest("mode must be one of SOLO, COOP, PVP.");
        }
        return mode;
    }

    private String requireText(String value, String fieldName, int minLength, int maxLength) {
        if (value == null) {
            throw badRequest(fieldName + " is required.");
        }
        String normalized = value.trim();
        if (normalized.length() < minLength || normalized.length() > maxLength) {
            throw badRequest(fieldName + " length must be between " + minLength + " and " + maxLength + ".");
        }
        return normalized;
    }

    private int requireNonNegativeInt(Object value, String fieldName) {
        if (!(value instanceof Number)) {
            throw badRequest(fieldName + " must be a number.");
        }
        int parsed = ((Number) value).intValue();
        if (parsed < 0) {
            throw badRequest(fieldName + " must be >= 0.");
        }
        return parsed;
    }

    private String shortId(String id) {
        if (id == null || id.isBlank()) {
            return "unknown";
        }
        return id.length() <= 5 ? id : id.substring(0, 5);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
