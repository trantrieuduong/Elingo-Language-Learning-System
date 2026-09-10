package com.elingo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ElingoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElingoApplication.class, args);
    }

}
