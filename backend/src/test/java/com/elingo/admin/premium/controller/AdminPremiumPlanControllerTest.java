package com.elingo.admin.premium.controller;

import com.elingo.admin.premium.service.AdminPremiumPlanService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.premium.dto.request.CreatePremiumPlanRequest;
import com.elingo.premium.dto.request.UpdatePremiumPlanRequest;
import com.elingo.premium.dto.response.AdminPremiumPlanResponse;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("AdminPremiumPlanController")
class AdminPremiumPlanControllerTest {

    @Mock
    private AdminPremiumPlanService adminPremiumPlanService;

    @InjectMocks
    private AdminPremiumPlanController controller;

    private static final AdminPremiumPlanResponse SAMPLE_PLAN = new AdminPremiumPlanResponse(
            1L, "Pro Monthly", new BigDecimal("99000.00"), "VND", 30, false,
            LocalDateTime.now(), LocalDateTime.now()
    );

    @Nested
    @DisplayName("getAllPlans")
    class GetAllPlans {

        @Test
        @DisplayName("should return success ApiResponse with plan list")
        void shouldReturnAllPlans() {
            when(adminPremiumPlanService.getAllPlans()).thenReturn(List.of(SAMPLE_PLAN));

            ApiResponse<List<AdminPremiumPlanResponse>> response = controller.getAllPlans();

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).containsExactly(SAMPLE_PLAN);
            verify(adminPremiumPlanService).getAllPlans();
        }
    }

    @Nested
    @DisplayName("getPlanById")
    class GetPlanById {

        @Test
        @DisplayName("should return success ApiResponse with plan details")
        void shouldReturnPlanById() {
            when(adminPremiumPlanService.getPlanById(1L)).thenReturn(SAMPLE_PLAN);

            ApiResponse<AdminPremiumPlanResponse> response = controller.getPlanById(1L);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(SAMPLE_PLAN);
        }
    }

    @Nested
    @DisplayName("createPlan")
    class CreatePlan {

        @Test
        @DisplayName("should return created plan")
        void shouldCreatePlan() {
            CreatePremiumPlanRequest request = new CreatePremiumPlanRequest(
                    "Pro Monthly", new BigDecimal("99000.00"), "VND", 30
            );
            when(adminPremiumPlanService.createPlan(request)).thenReturn(SAMPLE_PLAN);

            ApiResponse<AdminPremiumPlanResponse> response = controller.createPlan(request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(SAMPLE_PLAN);
        }
    }

    @Nested
    @DisplayName("updatePlan")
    class UpdatePlan {

        @Test
        @DisplayName("should return updated plan")
        void shouldUpdatePlan() {
            UpdatePremiumPlanRequest request = new UpdatePremiumPlanRequest("Pro Updated", new BigDecimal("109000.00"));
            when(adminPremiumPlanService.updatePlan(1L, request)).thenReturn(SAMPLE_PLAN);

            ApiResponse<AdminPremiumPlanResponse> response = controller.updatePlan(1L, request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(SAMPLE_PLAN);
        }
    }

    @Nested
    @DisplayName("disablePlan")
    class DisablePlan {

        @Test
        @DisplayName("should return disabled plan")
        void shouldDisablePlan() {
            when(adminPremiumPlanService.disablePlan(1L)).thenReturn(SAMPLE_PLAN);

            ApiResponse<AdminPremiumPlanResponse> response = controller.disablePlan(1L);

            assertThat(response.isSuccess()).isTrue();
            verify(adminPremiumPlanService).disablePlan(1L);
        }
    }

    @Nested
    @DisplayName("enablePlan")
    class EnablePlan {

        @Test
        @DisplayName("should return enabled plan")
        void shouldEnablePlan() {
            when(adminPremiumPlanService.enablePlan(1L)).thenReturn(SAMPLE_PLAN);

            ApiResponse<AdminPremiumPlanResponse> response = controller.enablePlan(1L);

            assertThat(response.isSuccess()).isTrue();
            verify(adminPremiumPlanService).enablePlan(1L);
        }
    }
}
