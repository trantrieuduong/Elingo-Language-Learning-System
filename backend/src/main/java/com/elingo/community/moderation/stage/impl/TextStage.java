package com.elingo.community.moderation.stage.impl;

import com.elingo.community.moderation.model.ModerationResult;
import com.elingo.community.moderation.model.PostForModeration;
import com.elingo.community.moderation.moderator.TextModerator;
import java.util.List;
import java.util.Optional;

import com.elingo.community.moderation.stage.ModerationStage;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class TextStage implements ModerationStage {

    private final List<TextModerator> moderators; // đã sắp theo @Order, moderator mặc định (OpenAI) đứng cuối

    public TextStage(List<TextModerator> moderators) {
        this.moderators = moderators;
    }

    @Override
    public String name() {
        return "TEXT";
    }

    @Override
    public Optional<ModerationResult> evaluate(PostForModeration post) {
        String title = post.title();
        String content = post.content();
        String text = title + " " + content;

        if (text.isBlank())
            return Optional.empty();

        TextModerator moderator = moderators.stream()
                .filter(m -> m.supports(text))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("None of TextModerator support this text"));
        return Optional.of(moderator.moderateText(text));
    }
}
