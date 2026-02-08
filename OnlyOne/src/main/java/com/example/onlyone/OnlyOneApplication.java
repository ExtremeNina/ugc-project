package com.example.onlyone;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.example.onlyone.Mapper")
@EnableScheduling
public class OnlyOneApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnlyOneApplication.class, args);
    }

}
