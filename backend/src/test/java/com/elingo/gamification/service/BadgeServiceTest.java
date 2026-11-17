package com.elingo.gamification.service;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.gamification.dto.response.UserBadgeEarnedResponse;
import com.elingo.gamification.entity.Badge;
import com.elingo.gamification.entity.BadgeConditionType;
import com.elingo.gamification.entity.UserBadge;
import com.elingo.gamification.mapper.BadgeMapper;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class BadgeServiceTest {

    @Mock
    private UserBadgeRepository userBadgeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BadgeMapper badgeMapper;

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

            Badge badge1 = Badge.builder()
                    .code("STREAK_7_DAYS")
                    .name("7-Day Streak Master")
                    .description("Maintain a learning streak for 7 consecutive days")
                    .iconUrl("https://cdn.elingo.com/badges/streak-7.png")
                    .conditionType(BadgeConditionType.STREAK_DAYS)
                    .conditionValue(7)
                    .build();
            badge1.setId(101L);

            Badge badge2 = Badge.builder()
                    .code("TOTAL_XP_1000")
                    .name("XP Novice")
                    .description("Accumulate 1,000 total experience points")
                    .iconUrl("https://cdn.elingo.com/badges/xp-1000.png")
                    .conditionType(BadgeConditionType.TOTAL_XP)
                    .conditionValue(1000)
                    .build();
            badge2.setId(102L);

            User user = User.builder().build();
            user.setId(USER_ID);

            UserBadge userBadge1 = UserBadge.builder()
                    .user(user)
                    .badge(badge1)
                    .earnedAt(earnedTime1)
                    .build();
            userBadge1.setId(1L);

            UserBadge userBadge2 = UserBadge.builder()
                    .user(user)
                    .badge(badge2)
                    .earnedAt(earnedTime2)
                    .build();
            userBadge2.setId(2L);

            UserBadgeEarnedResponse response1 = new UserBadgeEarnedResponse(
                    101L,
                    "STREAK_7_DAYS",
                    "7-Day Streak Master",
                    "Maintain a learning streak for 7 consecutive days",
                    "https://cdn.elingo.com/badges/streak-7.png",
                    BadgeConditionType.STREAK_DAYS,
                    7,
                    earnedTime1
            );

            UserBadgeEarnedResponse response2 = new UserBadgeEarnedResponse(
                    102L,
                    "TOTAL_XP_1000",
                    "XP Novice",
                    "Accumulate 1,000 total experience points",
                    "https://cdn.elingo.com/badges/xp-1000.png",
                    BadgeConditionType.TOTAL_XP,
                    1000,
                    earnedTime2
            );

            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(userBadgeRepository.findAllByUserIdWithBadge(USER_ID)).thenReturn(List.of(userBadge1, userBadge2));
            when(badgeMapper.toBadgeResponse(badge1, earnedTime1)).thenReturn(response1);
            when(badgeMapper.toBadgeResponse(badge2, earnedTime2)).thenReturn(response2);

            List<UserBadgeEarnedResponse> responses = badgeService.getUserBadges(USER_ID);

            assertThat(responses).isNotNull().hasSize(2);
            assertThat(responses.get(0)).isEqualTo(response1);
            assertThat(responses.get(1)).isEqualTo(response2);

            verify(userRepository).existsById(USER_ID);
            verify(userBadgeRepository).findAllByUserIdWithBadge(USER_ID);
            verify(badgeMapper).toBadgeResponse(badge1, earnedTime1);
            verify(badgeMapper).toBadgeResponse(badge2, earnedTime2);
        }

        @Test
        @DisplayName("Get earned badges successfully when user has not earned any badges yet")
        void getUserBadges_Success_WhenUserHasNoEarnedBadges_ReturnsEmptyList() {
            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(userBadgeRepository.findAllByUserIdWithBadge(USER_ID)).thenReturn(List.of());

            List<UserBadgeEarnedResponse> responses = badgeService.getUserBadges(USER_ID);

            assertThat(responses).isNotNull().isEmpty();
            verify(userRepository).existsById(USER_ID);
            verify(userBadgeRepository).findAllByUserIdWithBadge(USER_ID);
            verifyNoInteractions(badgeMapper);
        }
    }

    @Nested
    @DisplayName("getUserBadges - Failure cases")
    class GetUserBadgesFailureTests {

        @Test
        @DisplayName("Throws AppException when userId is null")
        void getUserBadges_Fail_WhenUserIdIsNull_ThrowsAppException() {
            assertThatThrownBy(() -> badgeService.getUserBadges(null))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));

            verify(userRepository, never()).existsById(any());
            verify(userBadgeRepository, never()).findAllByUserIdWithBadge(anyLong());
            verifyNoInteractions(badgeMapper);
        }

        @Test
        @DisplayName("Throws AppException when user does not exist in database")
        void getUserBadges_Fail_WhenUserNotFoundInDatabase_ThrowsAppException() {
            Long nonExistentUserId = 999L;
            when(userRepository.existsById(nonExistentUserId)).thenReturn(false);

            assertThatThrownBy(() -> badgeService.getUserBadges(nonExistentUserId))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));

            verify(userRepository).existsById(nonExistentUserId);
            verify(userBadgeRepository, never()).findAllByUserIdWithBadge(anyLong());
            verifyNoInteractions(badgeMapper);
        }
    }
}
