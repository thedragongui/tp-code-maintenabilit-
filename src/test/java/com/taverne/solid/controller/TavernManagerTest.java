package com.taverne.solid.controller;

import com.epsi.Main;
import com.taverne.solid.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {Main.class, TavernManagerTest.FixedClockConfig.class})
class TavernManagerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private OrderService orderService;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void placeOrder_withValidPayload_returnsTotalAndStoresOrder() throws Exception {
        String payload = """
                {
                  "items": [
                    { "itemName": "bread", "unitPrice": 10, "quantity": 2 }
                  ]
                }
                """;

        mvc.perform(post("/api/solid/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(21.0));

        assertTrue(orderService.getOrderHistory().size() > 0);
    }

    @Test
    void placeOrder_withInsufficientStock_returnsBadRequest() throws Exception {
        String payload = """
                {
                  "items": [
                    { "itemName": "stew", "unitPrice": 12, "quantity": 999 }
                  ]
                }
                """;

        mvc.perform(post("/api/solid/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void placeOrder_withInvalidQuantity_returnsBadRequest() throws Exception {
        String payload = """
                {
                  "items": [
                    { "itemName": "ale", "unitPrice": 7, "quantity": 0 }
                  ]
                }
                """;

        mvc.perform(post("/api/solid/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-05-04T12:00:00Z"), ZoneId.of("Europe/Paris"));
        }
    }
}
