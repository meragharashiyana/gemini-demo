package com.example.gemini_demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class HelloControllerDbIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void dbHelloShouldReturnMessageFromDatabase() throws Exception {
        mockMvc.perform(get("/api/db-hello"))
                .andExpect(status().isOk())
                .andExpect(content().string(in(new String[]{"Hello from the database!", "Database says hi!", "Greetings from H2!"})));
    }

    @Test
    void usersShouldReturnSeededUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", hasItems("alice", "bob")));
    }

    @Test
    void migrationsEndpointShouldReturnFlywayInfo() throws Exception {
        mockMvc.perform(get("/api/migrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current.version", is("1")))
                .andExpect(jsonPath("$.applied[0].version", is("1")));
    }
}

