package com.elingo.admin.premium.service.impl;

import com.elingo.admin.audit.annotation.Audited;
import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import com.elingo.admin.premium.service.AdminPremiumPlanService;
import com.elingo.premium.dto.request.CreatePremiumPlanRequest;
import com.elingo.premium.dto.request.UpdatePremiumPlanRequest;
import com.elingo.premium.dto.response.AdminPremiumPlanResponse;
import com.elingo.premium.service.PremiumAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ADMIN-PREMIUM-PLAN-SERVICE")
public class AdminPremiumPlanServiceImpl implements AdminPremiumPlanService {

    private final PremiumAdminService premiumAdminService;

    @Override
    public List<AdminPremiumPlanResponse> getAllPlans() {
        return premiumAdminService.getAllPlansForAdmin();
    }

    @Override
    public AdminPremiumPlanResponse getPlanById(Long planId) {
        return premiumAdminService.getPlanByIdForAdmin(planId);
    }

    @Override
    @Audited(
            action = AdminAuditAction.PLAN_CREATE,
            target = AdminAuditTargetType.PREMIUM_PLAN,
            targetId = "#result.id",
            detail = "'Create plan name=' + #request.name() + ' price=' + #request.price() + ' durationDays=' + #request.durationDays()"
    )
    public AdminPremiumPlanResponse createPlan(CreatePremiumPlanRequest request) {
        AdminPremiumPlanResponse created = premiumAdminService.createPlan(request);
        log.info("Premium plan created planId={} durationDays={} isActive={}",
                created.id(), created.durationDays(), created.isActive());
        return created;
    }

    @Override
    @Audited(
            action = AdminAuditAction.PLAN_UPDATE,
            target = AdminAuditTargetType.PREMIUM_PLAN,
            targetId = "#planId",
            detail = "'Update plan name=' + #request.name() + ' price=' + #request.price()"
    )
    public AdminPremiumPlanResponse updatePlan(Long planId, UpdatePremiumPlanRequest request) {
        AdminPremiumPlanResponse updated = premiumAdminService.updatePlan(planId, request);
        log.info("Premium plan updated planId={} price={}", planId, updated.price());
        return updated;
    }

    @Override
    @Audited(
            action = AdminAuditAction.PLAN_DISABLE,
            target = AdminAuditTargetType.PREMIUM_PLAN,
            targetId = "#planId",
            detail = "'Disable plan id=' + #planId"
    )
    public AdminPremiumPlanResponse disablePlan(Long planId) {
        AdminPremiumPlanResponse disabled = premiumAdminService.disablePlan(planId);
        log.info("Premium plan disabled planId={}", planId);
        return disabled;
    }

    @Override
    @Audited(
            action = AdminAuditAction.PLAN_ENABLE,
            target = AdminAuditTargetType.PREMIUM_PLAN,
            targetId = "#planId",
            detail = "'Enable plan id=' + #planId"
    )
    public AdminPremiumPlanResponse enablePlan(Long planId) {
        AdminPremiumPlanResponse enabled = premiumAdminService.enablePlan(planId);
        log.info("Premium plan enabled planId={}", planId);
        return enabled;
    }
}
