package com.example.gemini_demo.service;

import com.example.gemini_demo.mapper.GreetingMapper;
import com.example.gemini_demo.mapper.UserMapper;
import com.example.gemini_demo.model.Greeting;
import com.example.gemini_demo.model.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HelloService {

    private final GreetingMapper greetingMapper;
    private final UserMapper userMapper;

    public HelloService(GreetingMapper greetingMapper, UserMapper userMapper) {
        this.greetingMapper = greetingMapper;
        this.userMapper = userMapper;
    }

    public String getGreetingFromDb() {
        Greeting greeting = greetingMapper.getRandomGreeting();
        return greeting.getMessage();
    }

    public List<User> getAllUsers() {
        return userMapper.findAllUsers();
    }
}
