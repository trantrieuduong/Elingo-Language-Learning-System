package com.elingo;

import com.elingo.battle.config.BattleProperties;
import com.elingo.community.moderation.config.ModerationProperties;
import com.elingo.config.ThreadPoolProperties;
import com.elingo.vocabulary.config.FlashcardProperties;
import com.elingo.learning.lesson.config.LessonProperties;
import com.elingo.speaking.config.SpeakingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.TimeZone;

@SpringBootApplication
@EnableConfigurationProperties({
        FlashcardProperties.class,
        LessonProperties.class,
        BattleProperties.class,
        ModerationProperties.class,
        ThreadPoolProperties.class,
        SpeakingProperties.class
})
public class ElingoApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SpringApplication.run(ElingoApplication.class, args);
    }

}
