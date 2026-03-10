package com.example.gemini_demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HelloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void helloEndpointShouldReturnSuccessOrFallback() throws Exception {
        // 1. Initiate the request
        // Since the controller returns CompletableFuture, the request starts asynchronously
        MvcResult mvcResult = mockMvc.perform(get("/api/hello"))
                .andExpect(request().asyncStarted())
                .andReturn();

        // This forces the test to wait max 1000ms, otherwise it throws an error
        mvcResult.getAsyncResult(1000);

        // 2. Dispatch the async result and verify the response
        // We use 'anyOf' to accept either the success message OR the fallback message
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().string(anyOf(
                        containsString("Hello from Spring Boot!"),
                        containsString("Fallback: Service is currently busy")
                )));
    }
}