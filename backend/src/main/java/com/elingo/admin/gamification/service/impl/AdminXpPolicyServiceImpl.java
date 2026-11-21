package com.elingo.admin.gamification.service.impl;

import com.elingo.admin.audit.annotation.Audited;
import com.elingo.admin.audit.aspect.CurrentAdminProvider;
import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import com.elingo.admin.gamification.dto.request.UpdateXpPolicyRequest;
import com.elingo.admin.gamification.dto.response.AdminXpPolicyResponse;
import com.elingo.admin.gamification.service.AdminXpPolicyService;
import com.elingo.gamification.entity.XpSource;
import com.elingo.gamification.service.XpPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ADMIN-XP-POLICY-SERVICE")
public class AdminXpPolicyServiceImpl implements AdminXpPolicyService {

    private final XpPolicy xpPolicy;
    private final CurrentAdminProvider currentAdminProvider;

    @Override
    public List<AdminXpPolicyResponse> getAllPolicies() {
        Map<XpSource, XpPolicy.Rule> rules = xpPolicy.getAllRules();
        List<AdminXpPolicyResponse> result = new ArrayList<>();
        for (Map.Entry<XpSource, XpPolicy.Rule> entry : rules.entrySet()) {
            XpPolicy.Rule r = entry.getValue();
            result.add(new AdminXpPolicyResponse(entry.getKey(), r.xp(), r.dailyLimit()));
        }
        return result;
    }

    @Override
    @Audited(
            action = AdminAuditAction.XP_POLICY_UPDATE,
            target = AdminAuditTargetType.SYSTEM_CONFIG,
            targetId = "#source.name()",
            detail = "'XP policy: ' + #source + ' xp=' + #request.xp + ' limit=' + #request.dailyLimit"
    )
    public AdminXpPolicyResponse updatePolicy(XpSource source, UpdateXpPolicyRequest request) {
        Long adminId = currentAdminProvider.id();
        xpPolicy.updateRule(source, request.xp(), request.dailyLimit(), adminId);
        return new AdminXpPolicyResponse(source, request.xp(), request.dailyLimit());
    }
}
