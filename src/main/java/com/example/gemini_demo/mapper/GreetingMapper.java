package com.example.gemini_demo.mapper;

import com.example.gemini_demo.model.Greeting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GreetingMapper {

    @Select("SELECT * FROM greetings ORDER BY RAND() LIMIT 1")
    Greeting getRandomGreeting();
}
