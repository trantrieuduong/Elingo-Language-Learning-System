package com.elingo.admin.gamification.controller;

import com.elingo.gamification.dto.request.BadgeRequest;
import com.elingo.gamification.dto.response.AdminBadgeResponse;
import com.elingo.admin.gamification.service.AdminBadgeService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.gamification.entity.BadgeConditionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("AdminBadgeController Tests")
class AdminBadgeControllerTest {

    @Mock
    private AdminBadgeService adminBadgeService;

    @InjectMocks
    private AdminBadgeController controller;

    private static final Long BADGE_ID = 10L;
    private static final AdminBadgeResponse SAMPLE_RESPONSE = new AdminBadgeResponse(
            BADGE_ID, "streak_7", "7 Days Streak", "Description", "icon.png",
            BadgeConditionType.STREAK_DAYS, 7, LocalDateTime.now()
    );

    @Nested
    @DisplayName("getAllBadges Tests")
    class GetAllBadgesTests {

        @Test
        @DisplayName("Should return all badges when no filter is provided")
        void getAllBadges_NoFilter_Success() {
            when(adminBadgeService.getAllBadges(null)).thenReturn(List.of(SAMPLE_RESPONSE));

            ApiResponse<List<AdminBadgeResponse>> response = controller.getAllBadges(null);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).hasSize(1);
            verify(adminBadgeService).getAllBadges(null);
        }

        @Test
        @DisplayName("Should return filtered badges when conditionType is provided")
        void getAllBadges_WithFilter_Success() {
            when(adminBadgeService.getAllBadges(BadgeConditionType.STREAK_DAYS))
                    .thenReturn(List.of(SAMPLE_RESPONSE));

            ApiResponse<List<AdminBadgeResponse>> response = controller.getAllBadges(BadgeConditionType.STREAK_DAYS);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).hasSize(1);
            verify(adminBadgeService).getAllBadges(BadgeConditionType.STREAK_DAYS);
        }
    }

    @Nested
    @DisplayName("getConditionTypes Tests")
    class GetConditionTypesTests {

        @Test
        @DisplayName("Should return all supported condition types")
        void getConditionTypes_Success() {
            when(adminBadgeService.getConditionTypes()).thenReturn(List.of(BadgeConditionType.values()));

            ApiResponse<List<BadgeConditionType>> response = controller.getConditionTypes();

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).hasSize(BadgeConditionType.values().length);
            verify(adminBadgeService).getConditionTypes();
        }
    }

    @Nested
    @DisplayName("getBadgeById Tests")
    class GetBadgeByIdTests {

        @Test
        @DisplayName("Should return badge when badge exists")
        void getBadgeById_Success() {
            when(adminBadgeService.getBadgeById(BADGE_ID)).thenReturn(SAMPLE_RESPONSE);

            ApiResponse<AdminBadgeResponse> response = controller.getBadgeById(BADGE_ID);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().id()).isEqualTo(BADGE_ID);
            verify(adminBadgeService).getBadgeById(BADGE_ID);
        }

        @Test
        @DisplayName("Should throw BADGE_NOT_FOUND when badge does not exist")
        void getBadgeById_NotFound_ThrowsException() {
            when(adminBadgeService.getBadgeById(999L))
                    .thenThrow(new AppException(AppError.BADGE_NOT_FOUND));

            assertThatThrownBy(() -> controller.getBadgeById(999L))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.BADGE_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("createBadge Tests")
    class CreateBadgeTests {

        @Test
        @DisplayName("Should create badge successfully when request is valid")
        void createBadge_Success() {
            BadgeRequest request = new BadgeRequest("STREAK_7", "7 Days Streak", "Description", "icon.png", BadgeConditionType.STREAK_DAYS, 7);
            when(adminBadgeService.createBadge(request)).thenReturn(SAMPLE_RESPONSE);

            ApiResponse<AdminBadgeResponse> response = controller.createBadge(request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().code()).isEqualTo("streak_7");
            verify(adminBadgeService).createBadge(request);
        }

        @Test
        @DisplayName("Should throw BADGE_CODE_EXISTED when code is duplicated")
        void createBadge_CodeExisted_ThrowsException() {
            BadgeRequest request = new BadgeRequest("STREAK_7", "7 Days Streak", "Description", "icon.png", BadgeConditionType.STREAK_DAYS, 7);
            when(adminBadgeService.createBadge(request))
                    .thenThrow(new AppException(AppError.BADGE_CODE_EXISTED));

            assertThatThrownBy(() -> controller.createBadge(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.BADGE_CODE_EXISTED));
        }
    }

    @Nested
    @DisplayName("updateBadge Tests")
    class UpdateBadgeTests {

        @Test
        @DisplayName("Should update badge successfully (PUT full update)")
        void updateBadge_Success() {
            BadgeRequest request = new BadgeRequest("STREAK_14", "14 Days", "Updated", "new_icon.png", BadgeConditionType.STREAK_DAYS, 14);
            AdminBadgeResponse updatedResponse = new AdminBadgeResponse(
                    BADGE_ID, "STREAK_14", "14 Days", "Updated", "new_icon.png",
                    BadgeConditionType.STREAK_DAYS, 14, LocalDateTime.now()
            );
            when(adminBadgeService.updateBadge(BADGE_ID, request)).thenReturn(updatedResponse);

            ApiResponse<AdminBadgeResponse> response = controller.updateBadge(BADGE_ID, request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().code()).isEqualTo("STREAK_14");
            verify(adminBadgeService).updateBadge(BADGE_ID, request);
        }

        @Test
        @DisplayName("Should throw BADGE_CODE_EXISTED when updating to existing code of another badge")
        void updateBadge_CodeExisted_ThrowsException() {
            BadgeRequest request = new BadgeRequest("EXISTING_CODE", "Name", "Desc", "icon.png", BadgeConditionType.STREAK_DAYS, 7);
            when(adminBadgeService.updateBadge(BADGE_ID, request))
                    .thenThrow(new AppException(AppError.BADGE_CODE_EXISTED));

            assertThatThrownBy(() -> controller.updateBadge(BADGE_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.BADGE_CODE_EXISTED));
        }
    }

    @Nested
    @DisplayName("deleteBadge Tests")
    class DeleteBadgeTests {

        @Test
        @DisplayName("Should delete badge successfully when not in use")
        void deleteBadge_Success() {
            ApiResponse<Void> response = controller.deleteBadge(BADGE_ID);

            assertThat(response.isSuccess()).isTrue();
            verify(adminBadgeService).deleteBadge(BADGE_ID);
        }

        @Test
        @DisplayName("Should throw BADGE_IN_USE when badge is held by users (foreign key check)")
        void deleteBadge_BadgeInUse_ThrowsException() {
            doThrow(new AppException(AppError.BADGE_IN_USE))
                    .when(adminBadgeService).deleteBadge(BADGE_ID);

            assertThatThrownBy(() -> controller.deleteBadge(BADGE_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.BADGE_IN_USE));
        }
    }
}
