package com.example.coursework;

import com.example.coursework.api.BibleSource;
import com.example.coursework.api.CatFactsBreedsSource;
import com.example.coursework.model.ApiSource;
import com.example.coursework.api.JsonPlaceholderUsersSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.util.List;

@SpringBootApplication
public class CourseworkApplication {

    public static void main(String[] args) {
        SpringApplication.run(CourseworkApplication.class, args);
    }

    @Bean
    public List<ApiSource> apiSources() {
        return List.of();
    }
}