package com.elingo.admin.premium.service;

import com.elingo.premium.dto.request.CreatePremiumPlanRequest;
import com.elingo.premium.dto.request.UpdatePremiumPlanRequest;
import com.elingo.premium.dto.response.AdminPremiumPlanResponse;

import java.util.List;

public interface AdminPremiumPlanService {
    List<AdminPremiumPlanResponse> getAllPlans();
    AdminPremiumPlanResponse getPlanById(Long planId);
    AdminPremiumPlanResponse createPlan(CreatePremiumPlanRequest request);
    AdminPremiumPlanResponse updatePlan(Long planId, UpdatePremiumPlanRequest request);
    AdminPremiumPlanResponse disablePlan(Long planId);
    AdminPremiumPlanResponse enablePlan(Long planId);
}
