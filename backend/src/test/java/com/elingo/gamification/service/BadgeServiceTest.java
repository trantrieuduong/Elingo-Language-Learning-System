package com.elingo.gamification.service;

import com.elingo.gamification.dto.request.BadgeRequest;
import com.elingo.gamification.dto.response.AdminBadgeResponse;
import com.elingo.common.enums.SystemFileModule;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.file.util.MediaKeyResolver;
import com.elingo.gamification.dto.response.UserBadgeEarnedResponse;
import com.elingo.gamification.entity.Badge;
import com.elingo.gamification.entity.BadgeConditionType;
import com.elingo.gamification.entity.UserBadge;
import com.elingo.gamification.mapper.BadgeMapper;
import com.elingo.gamification.repository.BadgeRepository;
import com.elingo.gamification.repository.UserBadgeRepository;
import com.elingo.gamification.service.impl.BadgeServiceImpl;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("BadgeService Tests")
class BadgeServiceTest {

    @Mock
    private UserBadgeRepository userBadgeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BadgeMapper badgeMapper;

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private com.elingo.gamification.catalog.BadgeCatalog badgeCatalog;

    @Mock
    private MediaKeyResolver mediaKeyResolver;

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private BadgeServiceImpl badgeService;

    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("getUserBadges - Success cases")
    class GetUserBadgesSuccessTests {

        @Test
        @DisplayName("Get earned badges successfully when user has earned badges")
        void getUserBadges_Success_WhenUserHasEarnedBadges() {
            LocalDateTime earnedTime1 = LocalDateTime.of(2026, 11, 16, 10, 0);
            LocalDateTime earnedTime2 = LocalDateTime.of(2026, 11, 15, 14, 30);

            User mockUser = User.builder().build();
            mockUser.setId(USER_ID);

            Badge badge1 = Badge.builder()
                    .code("first_login")
                    .name("First Login")
                    .description("Awarded for logging in the first time")
                    .iconUrl("http://example.com/badge1.png")
                    .conditionType(BadgeConditionType.TOTAL_XP)
                    .conditionValue(10)
                    .build();
            badge1.setId(101L);

            Badge badge2 = Badge.builder()
                    .code("streak_7")
                    .name("7-Day Streak")
                    .description("Maintained a 7-day learning streak")
                    .iconUrl("http://example.com/badge2.png")
                    .conditionType(BadgeConditionType.STREAK_DAYS)
                    .conditionValue(7)
                    .build();
            badge2.setId(102L);

            UserBadge userBadge1 = UserBadge.builder()
                    .user(mockUser)
                    .badge(badge1)
                    .earnedAt(earnedTime1)
                    .build();

            UserBadge userBadge2 = UserBadge.builder()
                    .user(mockUser)
                    .badge(badge2)
                    .earnedAt(earnedTime2)
                    .build();

            UserBadgeEarnedResponse response1 = new UserBadgeEarnedResponse(
                    101L, "first_login", "First Login", "Awarded for logging in the first time",
                    "http://example.com/badge1.png", BadgeConditionType.TOTAL_XP, 10, earnedTime1
            );
            UserBadgeEarnedResponse response2 = new UserBadgeEarnedResponse(
                    102L, "streak_7", "7-Day Streak", "Maintained a 7-day learning streak",
                    "http://example.com/badge2.png", BadgeConditionType.STREAK_DAYS, 7, earnedTime2
            );

            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(userBadgeRepository.findAllByUserIdWithBadge(USER_ID)).thenReturn(List.of(userBadge1, userBadge2));
            when(badgeMapper.toBadgeResponse(badge1, earnedTime1)).thenReturn(response1);
            when(badgeMapper.toBadgeResponse(badge2, earnedTime2)).thenReturn(response2);

            List<UserBadgeEarnedResponse> result = badgeService.getUserBadges(USER_ID);

            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result.get(0)).isEqualTo(response1);
            assertThat(result.get(1)).isEqualTo(response2);

            verify(userRepository, times(1)).existsById(USER_ID);
            verify(userBadgeRepository, times(1)).findAllByUserIdWithBadge(USER_ID);
            verify(badgeMapper, times(1)).toBadgeResponse(badge1, earnedTime1);
            verify(badgeMapper, times(1)).toBadgeResponse(badge2, earnedTime2);
        }

        @Test
        @DisplayName("Get earned badges returns empty list when user has not earned any badges")
        void getUserBadges_Success_WhenUserHasNoBadges() {
            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(userBadgeRepository.findAllByUserIdWithBadge(USER_ID)).thenReturn(List.of());

            List<UserBadgeEarnedResponse> result = badgeService.getUserBadges(USER_ID);

            assertThat(result).isNotNull();
            assertThat(result).isEmpty();

            verify(userRepository, times(1)).existsById(USER_ID);
            verify(userBadgeRepository, times(1)).findAllByUserIdWithBadge(USER_ID);
            verifyNoInteractions(badgeMapper);
        }
    }

    @Nested
    @DisplayName("getUserBadges - Failure cases")
    class GetUserBadgesFailureTests {

        @Test
        @DisplayName("Should throw USER_NOT_FOUND exception when user ID is null")
        void getUserBadges_Fail_UserIdNull() {
            assertThatThrownBy(() -> badgeService.getUserBadges(null))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));

            verify(userRepository, never()).existsById(anyLong());
            verify(userBadgeRepository, never()).findAllByUserIdWithBadge(anyLong());
            verifyNoInteractions(badgeMapper);
        }

        @Test
        @DisplayName("Should throw USER_NOT_FOUND exception when user does not exist")
        void getUserBadges_Fail_UserDoesNotExist() {
            when(userRepository.existsById(USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> badgeService.getUserBadges(USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));

            verify(userRepository, times(1)).existsById(USER_ID);
            verify(userBadgeRepository, never()).findAllByUserIdWithBadge(anyLong());
            verifyNoInteractions(badgeMapper);
        }
    }

    @Nested
    @DisplayName("Admin Badge CRUD Operations")
    class AdminBadgeCrudTests {

        @Test
        @DisplayName("createBadge succeeds with valid inputs")
        void createBadge_Success() {
            BadgeRequest req = new BadgeRequest("STREAK_30", "30 Days Streak", "Description", "icon.png", BadgeConditionType.STREAK_DAYS, 30);

            when(badgeRepository.existsByCode("streak_30")).thenReturn(false);
            when(mediaKeyResolver.forCreate(eq("icon.png"), any(SystemFileModule.class))).thenReturn(
                    new MediaKeyResolver.Resolved("system/badges/icon.png", "icon.png", List.of()));
            Badge badgeToSave = Badge.builder().code("streak_30").name("30 Days Streak").description("Description").conditionType(BadgeConditionType.STREAK_DAYS).conditionValue(30).build();
            Badge saved = Badge.builder().code("streak_30").name("30 Days Streak").description("Description").conditionType(BadgeConditionType.STREAK_DAYS).conditionValue(30).build();
            saved.setId(200L);

            AdminBadgeResponse res = new AdminBadgeResponse(200L, "streak_30", "30 Days Streak", "Description", "system/badges/icon.png", BadgeConditionType.STREAK_DAYS, 30, LocalDateTime.now());

            when(badgeMapper.toBadge(req)).thenReturn(badgeToSave);
            when(badgeRepository.save(any(Badge.class))).thenReturn(saved);
            when(badgeMapper.toAdminBadgeResponse(saved)).thenReturn(res);

            AdminBadgeResponse badge = badgeService.createBadge(req);

            assertThat(badge.id()).isEqualTo(200L);
            assertThat(badge.code()).isEqualTo("streak_30");
            verify(badgeCatalog).reload();
            verify(mediaKeyResolver).publish(any());
        }

        @Test
        @DisplayName("getConditionTypes returns all supported condition types")
        void getConditionTypes_Success() {
            List<BadgeConditionType> types = badgeService.getConditionTypes();
            assertThat(types).containsExactly(BadgeConditionType.values());
        }

        @Test
        @DisplayName("createBadge fails when badge code already exists")
        void createBadge_Fail_CodeExisted() {
            BadgeRequest req = new BadgeRequest("STREAK_30", "30 Days", "Desc", "icon.png", BadgeConditionType.STREAK_DAYS, 30);
            when(badgeRepository.existsByCode("streak_30")).thenReturn(true);

            assertThatThrownBy(() -> badgeService.createBadge(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.BADGE_CODE_EXISTED));
        }

        @Test
        @DisplayName("updateBadge succeeds with full state replacement (PUT)")
        void updateBadge_Success() {
            Badge existing = Badge.builder().code("OLD_CODE").iconUrl("old.png").build();
            existing.setId(10L);
            when(badgeRepository.findById(10L)).thenReturn(Optional.of(existing));
            when(badgeRepository.existsByCodeAndIdNot("new_code", 10L)).thenReturn(false);
            when(mediaKeyResolver.forUpdate(eq("new.png"), eq("old.png"), any(SystemFileModule.class))).thenReturn(
                    new MediaKeyResolver.Resolved("system/badges/new.png", "new.png", List.of()));
            when(badgeRepository.save(any(Badge.class))).thenReturn(existing);

            AdminBadgeResponse res = new AdminBadgeResponse(10L, "new_code", "New Name", "Desc", "system/badges/new.png", BadgeConditionType.TOTAL_XP, 500, LocalDateTime.now());
            when(badgeMapper.toAdminBadgeResponse(existing)).thenReturn(res);

            BadgeRequest req = new BadgeRequest("NEW_CODE", "New Name", "Desc", "new.png", BadgeConditionType.TOTAL_XP, 500);
            AdminBadgeResponse updated = badgeService.updateBadge(10L, req);

            assertThat(updated.code()).isEqualTo("new_code");
            verify(badgeMapper).updateBadge(existing, req);
            verify(badgeCatalog).reload();
            verify(mediaKeyResolver).publish(any());
        }

        @Test
        @DisplayName("deleteBadge fails when badge is in use by users (foreign key constraint)")
        void deleteBadge_Fail_BadgeInUse() {
            Badge badge = Badge.builder().code("TOTAL_XP_100").build();
            badge.setId(10L);
            when(badgeRepository.findById(10L)).thenReturn(Optional.of(badge));
            when(userBadgeRepository.existsByBadgeId(10L)).thenReturn(true);

            assertThatThrownBy(() -> badgeService.deleteBadge(10L))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.BADGE_IN_USE));

            verify(badgeRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deleteBadge succeeds when badge is not in use")
        void deleteBadge_Success() {
            Badge badge = Badge.builder().code("TOTAL_XP_100").build();
            badge.setId(10L);
            when(badgeRepository.findById(10L)).thenReturn(Optional.of(badge));
            when(userBadgeRepository.existsByBadgeId(10L)).thenReturn(false);

            badgeService.deleteBadge(10L);

            verify(badgeRepository).delete(badge);
            verify(badgeCatalog).reload();
        }
    }
}
