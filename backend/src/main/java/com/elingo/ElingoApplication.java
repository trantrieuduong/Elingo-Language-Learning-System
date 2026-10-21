package com.elingo;

import com.elingo.gamification.config.GamificationProperties;
import com.elingo.learning.lesson.config.LessonProperties;
import com.elingo.learning.flashcard.config.FlashcardProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.TimeZone;

@SpringBootApplication
@EnableConfigurationProperties({
        FlashcardProperties.class,
        GamificationProperties.class,
        LessonProperties.class
})
public class ElingoApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SpringApplication.run(ElingoApplication.class, args);
    }

}
