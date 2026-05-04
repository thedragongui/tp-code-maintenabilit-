package com.taverne.dry.controller;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Main.class)
class PaymentControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void warrior_returnsExpectedAmount() throws Exception {
        mvc.perform(post("/api/payment/warrior")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":10,\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(content().string("33.50"));
    }

    @Test
    void mage_returnsExpectedAmount() throws Exception {
        mvc.perform(post("/api/payment/mage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":10,\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(content().string("30.00"));
    }

    @Test
    void rogue_returnsExpectedAmount() throws Exception {
        mvc.perform(post("/api/payment/rogue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":10,\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(content().string("31.50"));
    }

    @Test
    void invalidRequest_returnsBadRequest() throws Exception {
        mvc.perform(post("/api/payment/rogue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":-1,\"quantity\":3}"))
                .andExpect(status().isBadRequest());
    }
}
