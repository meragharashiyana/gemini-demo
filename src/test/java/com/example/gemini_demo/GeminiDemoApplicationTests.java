package com.example.gemini_demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GeminiDemoApplicationTests {

	@Autowired
	private TestRestTemplate template;

	@Test
	void contextLoads() {
	}

	@Test
	void shouldReturnHello() throws Exception {
		ResponseEntity<String> response = template.getForEntity("/api/hello", String.class);
		assertThat(response.getBody()).isIn(
				"Hello from Spring Boot!",
				"Fallback: Service is currently busy, please try again later."
		);
	}
}
