package com.elingo.gamification.config;

import com.elingo.gamification.entity.XpSource;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "app.gamification")
public record GamificationProperties(Map<XpSource, Rule> rules) {
    public record Rule(int xp, int dailyLimit) {}
    public Rule rule(XpSource source) { return rules.get(source); }
}
