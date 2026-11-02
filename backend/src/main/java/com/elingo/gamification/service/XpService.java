package com.elingo.gamification.service;

import com.elingo.gamification.entity.XpSource;

public interface XpService {
    void addXp(long userId, int amount, XpSource source, Long refId);
}
