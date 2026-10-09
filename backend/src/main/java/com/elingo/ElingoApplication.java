package com.elingo;

import com.elingo.config.FlashcardProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(FlashcardProperties.class)
public class ElingoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElingoApplication.class, args);
    }

}
