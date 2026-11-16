package com.elingo.gamification.service;

import com.elingo.gamification.dto.response.UserBadgeEarnedResponse;

import java.util.List;

public interface BadgeService {
    List<UserBadgeEarnedResponse> getUserBadges(Long userId);
}
