package com.elingo.admin.gamification.service.impl;

import com.elingo.admin.audit.annotation.Audited;
import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import com.elingo.gamification.dto.request.BadgeRequest;
import com.elingo.gamification.dto.response.AdminBadgeResponse;
import com.elingo.admin.gamification.service.AdminBadgeService;
import com.elingo.gamification.entity.BadgeConditionType;
import com.elingo.gamification.service.BadgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ADMIN-BADGE-SERVICE")
public class AdminBadgeServiceImpl implements AdminBadgeService {

    private final BadgeService badgeService;

    @Override
    public List<AdminBadgeResponse> getAllBadges(BadgeConditionType conditionType) {
        return badgeService.getAllBadgesForAdmin(conditionType);
    }

    @Override
    public AdminBadgeResponse getBadgeById(Long id) {
        return badgeService.getBadgeByIdForAdmin(id);
    }

    @Override
    @Audited(
            action = AdminAuditAction.BADGE_CREATE,
            target = AdminAuditTargetType.BADGE,
            targetId = "#result.id",
            detail = "'Create badge code: ' + #request.code"
    )
    public AdminBadgeResponse createBadge(BadgeRequest request) {
        AdminBadgeResponse created = badgeService.createBadge(request);
        log.info("Badge created badgeId={}", created.id());
        return created;
    }

    @Override
    @Audited(
            action = AdminAuditAction.BADGE_UPDATE,
            target = AdminAuditTargetType.BADGE,
            targetId = "#id",
            detail = "'Update badge id: ' + #id"
    )
    public AdminBadgeResponse updateBadge(Long id, BadgeRequest request) {
        AdminBadgeResponse updated = badgeService.updateBadge(id, request);
        log.info("Badge updated badgeId={}", id);
        return updated;
    }

    @Override
    @Audited(
            action = AdminAuditAction.BADGE_DELETE,
            target = AdminAuditTargetType.BADGE,
            targetId = "#id"
    )
    public void deleteBadge(Long id) {
        badgeService.deleteBadge(id);
        log.info("Badge deleted badgeId={}", id);
    }

    @Override
    public List<BadgeConditionType> getConditionTypes() {
        return badgeService.getConditionTypes();
    }
}
