package com.example.gemini_demo.service;


import com.example.gemini_demo.mapper.GreetingMapper;
import com.example.gemini_demo.model.Greeting;
import org.springframework.stereotype.Service;

@Service
public class HelloService {

    private final GreetingMapper greetingMapper;

    public HelloService(GreetingMapper greetingMapper) {
        this.greetingMapper = greetingMapper;
    }

    public String getGreetingFromDb() {
        Greeting greeting = greetingMapper.getRandomGreeting();
        return greeting.getMessage();
    }
}
