package com.elingo.admin.premium.service;

import com.elingo.admin.premium.service.impl.AdminPremiumPlanServiceImpl;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.premium.dto.request.CreatePremiumPlanRequest;
import com.elingo.premium.dto.request.UpdatePremiumPlanRequest;
import com.elingo.premium.dto.response.AdminPremiumPlanResponse;
import com.elingo.premium.service.PremiumAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("AdminPremiumPlanService")
class AdminPremiumPlanServiceTest {

    @Mock
    private PremiumAdminService premiumAdminService;

    @InjectMocks
    private AdminPremiumPlanServiceImpl adminPremiumPlanService;

    private static final AdminPremiumPlanResponse SAMPLE_PLAN = new AdminPremiumPlanResponse(
            1L, "Pro Monthly", new BigDecimal("99000.00"), "VND", 30, false,
            LocalDateTime.now(), LocalDateTime.now()
    );

    @Nested
    @DisplayName("getAllPlans")
    class GetAllPlans {

        @Test
        @DisplayName("should delegate to premiumAdminService.getAllPlansForAdmin")
        void shouldReturnPlans_whenCalled() {
            when(premiumAdminService.getAllPlansForAdmin()).thenReturn(List.of(SAMPLE_PLAN));

            List<AdminPremiumPlanResponse> result = adminPremiumPlanService.getAllPlans();

            assertThat(result).containsExactly(SAMPLE_PLAN);
            verify(premiumAdminService).getAllPlansForAdmin();
        }
    }

    @Nested
    @DisplayName("getPlanById")
    class GetPlanById {

        @Test
        @DisplayName("should return plan when found")
        void shouldReturnPlan_whenFound() {
            when(premiumAdminService.getPlanByIdForAdmin(1L)).thenReturn(SAMPLE_PLAN);

            AdminPremiumPlanResponse result = adminPremiumPlanService.getPlanById(1L);

            assertThat(result).isEqualTo(SAMPLE_PLAN);
        }

        @Test
        @DisplayName("should propagate PREMIUM_PLAN_NOT_FOUND when missing")
        void shouldThrowNotFound_whenMissing() {
            when(premiumAdminService.getPlanByIdForAdmin(99L))
                    .thenThrow(new AppException(AppError.PREMIUM_PLAN_NOT_FOUND));

            assertThatThrownBy(() -> adminPremiumPlanService.getPlanById(99L))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("appError", AppError.PREMIUM_PLAN_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("createPlan")
    class CreatePlan {

        @Test
        @DisplayName("should delegate createPlan to premiumAdminService")
        void shouldCreatePlan_whenValid() {
            CreatePremiumPlanRequest request = new CreatePremiumPlanRequest(
                    "Pro Monthly", new BigDecimal("99000.00"), "VND", 30
            );
            when(premiumAdminService.createPlan(request)).thenReturn(SAMPLE_PLAN);

            AdminPremiumPlanResponse result = adminPremiumPlanService.createPlan(request);

            assertThat(result).isEqualTo(SAMPLE_PLAN);
            verify(premiumAdminService).createPlan(request);
        }
    }

    @Nested
    @DisplayName("updatePlan")
    class UpdatePlan {

        @Test
        @DisplayName("should delegate updatePlan to premiumAdminService")
        void shouldUpdatePlan_whenValid() {
            UpdatePremiumPlanRequest request = new UpdatePremiumPlanRequest("Pro Updated", new BigDecimal("109000.00"));
            when(premiumAdminService.updatePlan(1L, request)).thenReturn(SAMPLE_PLAN);

            AdminPremiumPlanResponse result = adminPremiumPlanService.updatePlan(1L, request);

            assertThat(result).isEqualTo(SAMPLE_PLAN);
            verify(premiumAdminService).updatePlan(1L, request);
        }
    }

    @Nested
    @DisplayName("disablePlan")
    class DisablePlan {

        @Test
        @DisplayName("should delegate disablePlan to premiumAdminService")
        void shouldDisablePlan_whenActive() {
            when(premiumAdminService.disablePlan(1L)).thenReturn(SAMPLE_PLAN);

            AdminPremiumPlanResponse result = adminPremiumPlanService.disablePlan(1L);

            assertThat(result).isEqualTo(SAMPLE_PLAN);
            verify(premiumAdminService).disablePlan(1L);
        }
    }

    @Nested
    @DisplayName("enablePlan")
    class EnablePlan {

        @Test
        @DisplayName("should delegate enablePlan to premiumAdminService")
        void shouldEnablePlan_whenInactive() {
            when(premiumAdminService.enablePlan(1L)).thenReturn(SAMPLE_PLAN);

            AdminPremiumPlanResponse result = adminPremiumPlanService.enablePlan(1L);

            assertThat(result).isEqualTo(SAMPLE_PLAN);
            verify(premiumAdminService).enablePlan(1L);
        }
    }
}
