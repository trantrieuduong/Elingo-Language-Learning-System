package com.elingo.admin.gamification.service;

import com.elingo.gamification.dto.request.BadgeRequest;
import com.elingo.gamification.dto.response.AdminBadgeResponse;
import com.elingo.gamification.entity.BadgeConditionType;

import java.util.List;

public interface AdminBadgeService {

    List<AdminBadgeResponse> getAllBadges(BadgeConditionType conditionType);

    AdminBadgeResponse getBadgeById(Long id);

    AdminBadgeResponse createBadge(BadgeRequest request);

    AdminBadgeResponse updateBadge(Long id, BadgeRequest request);

    void deleteBadge(Long id);

    List<BadgeConditionType> getConditionTypes();
}
