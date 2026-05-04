package com.taverne.kiss.controller;

import com.epsi.Main;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Main.class)
class TabControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void calculate_withMealAndDrink_appliesDiscount() throws Exception {
        String payload = """
                {
                  "items": [
                    { "name": "Steak", "type": "MEAL", "price": 20.00 },
                    { "name": "Ale", "type": "DRINK", "price": 10.00 }
                  ]
                }
                """;

        mvc.perform(post("/api/tab/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(27.00));
    }

    @Test
    void calculate_withMealOnly_doesNotApplyDiscount() throws Exception {
        String payload = """
                {
                  "items": [
                    { "name": "Steak", "type": "MEAL", "price": 20.00 }
                  ]
                }
                """;

        mvc.perform(post("/api/tab/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(20.00));
    }

    @Test
    void calculate_withMissingItems_returnsBadRequest() throws Exception {
        mvc.perform(post("/api/tab/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void calculate_withNegativePrice_returnsBadRequest() throws Exception {
        String payload = """
                {
                  "items": [
                    { "name": "Invalid", "type": "DRINK", "price": -5.00 }
                  ]
                }
                """;

        mvc.perform(post("/api/tab/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }
}
