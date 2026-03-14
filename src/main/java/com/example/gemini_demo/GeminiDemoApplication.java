package com.example.gemini_demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableCaching
public class GeminiDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(GeminiDemoApplication.class, args);
	}

}
