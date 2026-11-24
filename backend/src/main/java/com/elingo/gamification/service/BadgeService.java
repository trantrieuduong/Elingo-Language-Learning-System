package com.elingo.gamification.service;

import com.elingo.gamification.dto.request.BadgeRequest;
import com.elingo.gamification.dto.response.AdminBadgeResponse;
import com.elingo.gamification.dto.response.UserBadgeEarnedResponse;
import com.elingo.gamification.entity.BadgeConditionType;

import java.util.List;

public interface BadgeService {

    List<UserBadgeEarnedResponse> getUserBadges(Long userId);

    // Admin
    List<AdminBadgeResponse> getAllBadgesForAdmin(BadgeConditionType conditionType);

    AdminBadgeResponse getBadgeByIdForAdmin(Long id);

    AdminBadgeResponse createBadge(BadgeRequest request);

    AdminBadgeResponse updateBadge(Long id, BadgeRequest request);

    void deleteBadge(Long id);

    List<BadgeConditionType> getConditionTypes();
}
