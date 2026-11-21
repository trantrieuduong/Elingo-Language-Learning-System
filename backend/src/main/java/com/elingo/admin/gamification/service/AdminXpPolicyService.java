package com.elingo.admin.gamification.service;

import com.elingo.admin.gamification.dto.request.UpdateXpPolicyRequest;
import com.elingo.admin.gamification.dto.response.AdminXpPolicyResponse;
import com.elingo.gamification.entity.XpSource;

import java.util.List;

public interface AdminXpPolicyService {

    List<AdminXpPolicyResponse> getAllPolicies();

    AdminXpPolicyResponse updatePolicy(XpSource source, UpdateXpPolicyRequest request);
}
