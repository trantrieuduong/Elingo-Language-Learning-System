package com.elingo.admin.gamification.service;

import com.elingo.gamification.dto.request.BadgeRequest;
import com.elingo.gamification.dto.response.AdminBadgeResponse;
import com.elingo.admin.gamification.service.impl.AdminBadgeServiceImpl;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.gamification.entity.BadgeConditionType;
import com.elingo.gamification.service.BadgeService;
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
@DisplayName("AdminBadgeService Tests")
class AdminBadgeServiceTest {

    @Mock
    private BadgeService badgeService;

    @InjectMocks
    private AdminBadgeServiceImpl adminBadgeService;

    private static final Long BADGE_ID = 10L;
    private static final AdminBadgeResponse SAMPLE_RESPONSE = new AdminBadgeResponse(
            BADGE_ID, "streak_7", "7 Days", "Description", "icon.png",
            BadgeConditionType.STREAK_DAYS, 7, LocalDateTime.now()
    );

    @Nested
    @DisplayName("getAllBadges Tests")
    class GetAllBadgesTests {

        @Test
        @DisplayName("Should delegate to badgeService.getAllBadgesForAdmin")
        void getAllBadges_Success() {
            when(badgeService.getAllBadgesForAdmin(BadgeConditionType.STREAK_DAYS))
                    .thenReturn(List.of(SAMPLE_RESPONSE));

            List<AdminBadgeResponse> result = adminBadgeService.getAllBadges(BadgeConditionType.STREAK_DAYS);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).code()).isEqualTo("streak_7");
            verify(badgeService).getAllBadgesForAdmin(BadgeConditionType.STREAK_DAYS);
        }
    }

    @Nested
    @DisplayName("getBadgeById Tests")
    class GetBadgeByIdTests {

        @Test
        @DisplayName("Should return badge response when badge exists")
        void getBadgeById_Success() {
            when(badgeService.getBadgeByIdForAdmin(BADGE_ID)).thenReturn(SAMPLE_RESPONSE);

            AdminBadgeResponse result = adminBadgeService.getBadgeById(BADGE_ID);

            assertThat(result.id()).isEqualTo(BADGE_ID);
            verify(badgeService).getBadgeByIdForAdmin(BADGE_ID);
        }

        @Test
        @DisplayName("Should propagate exception when badge not found")
        void getBadgeById_NotFound_ThrowsException() {
            when(badgeService.getBadgeByIdForAdmin(999L))
                    .thenThrow(new AppException(AppError.BADGE_NOT_FOUND));

            assertThatThrownBy(() -> adminBadgeService.getBadgeById(999L))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.BADGE_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("createBadge Tests")
    class CreateBadgeTests {

        @Test
        @DisplayName("Should delegate createBadge to badgeService and return response")
        void createBadge_Success() {
            BadgeRequest request = new BadgeRequest("STREAK_7", "7 Days", "Desc", "icon.png", BadgeConditionType.STREAK_DAYS, 7);
            when(badgeService.createBadge(request)).thenReturn(SAMPLE_RESPONSE);

            AdminBadgeResponse result = adminBadgeService.createBadge(request);

            assertThat(result.id()).isEqualTo(BADGE_ID);
            verify(badgeService).createBadge(request);
        }

        @Test
        @DisplayName("Should propagate exception when code already exists")
        void createBadge_ThrowsException() {
            BadgeRequest request = new BadgeRequest("STREAK_7", "7 Days", "Desc", "icon.png", BadgeConditionType.STREAK_DAYS, 7);
            when(badgeService.createBadge(request))
                    .thenThrow(new AppException(AppError.BADGE_CODE_EXISTED));

            assertThatThrownBy(() -> adminBadgeService.createBadge(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.BADGE_CODE_EXISTED));
        }
    }

    @Nested
    @DisplayName("updateBadge Tests")
    class UpdateBadgeTests {

        @Test
        @DisplayName("Should delegate updateBadge to badgeService and return response")
        void updateBadge_Success() {
            BadgeRequest request = new BadgeRequest("STREAK_7", "7 Days", "Desc", "icon.png", BadgeConditionType.STREAK_DAYS, 7);
            when(badgeService.updateBadge(BADGE_ID, request)).thenReturn(SAMPLE_RESPONSE);

            AdminBadgeResponse result = adminBadgeService.updateBadge(BADGE_ID, request);

            assertThat(result.id()).isEqualTo(BADGE_ID);
            verify(badgeService).updateBadge(BADGE_ID, request);
        }

        @Test
        @DisplayName("Should propagate exception when update code duplicates another badge")
        void updateBadge_ThrowsException() {
            BadgeRequest request = new BadgeRequest("EXISTING", "Name", "Desc", "icon.png", BadgeConditionType.STREAK_DAYS, 7);
            when(badgeService.updateBadge(BADGE_ID, request))
                    .thenThrow(new AppException(AppError.BADGE_CODE_EXISTED));

            assertThatThrownBy(() -> adminBadgeService.updateBadge(BADGE_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.BADGE_CODE_EXISTED));
        }
    }

    @Nested
    @DisplayName("deleteBadge Tests")
    class DeleteBadgeTests {

        @Test
        @DisplayName("Should delegate deleteBadge to badgeService")
        void deleteBadge_Success() {
            adminBadgeService.deleteBadge(BADGE_ID);

            verify(badgeService).deleteBadge(BADGE_ID);
        }

        @Test
        @DisplayName("Should propagate BADGE_IN_USE exception when badge has earned users")
        void deleteBadge_ThrowsException() {
            doThrow(new AppException(AppError.BADGE_IN_USE))
                    .when(badgeService).deleteBadge(BADGE_ID);

            assertThatThrownBy(() -> adminBadgeService.deleteBadge(BADGE_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.BADGE_IN_USE));
        }
    }

    @Nested
    @DisplayName("getConditionTypes Tests")
    class GetConditionTypesTests {

        @Test
        @DisplayName("Should delegate getConditionTypes to badgeService")
        void getConditionTypes_Success() {
            when(badgeService.getConditionTypes()).thenReturn(List.of(BadgeConditionType.values()));

            List<BadgeConditionType> result = adminBadgeService.getConditionTypes();

            assertThat(result).hasSize(BadgeConditionType.values().length);
            verify(badgeService).getConditionTypes();
        }
    }
}
