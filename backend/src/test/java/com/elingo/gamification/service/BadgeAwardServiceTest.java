package com.elingo.gamification.service;

import com.elingo.common.event.NotificationEvent;
import com.elingo.gamification.catalog.BadgeCatalog;
import com.elingo.gamification.entity.Badge;
import com.elingo.gamification.entity.BadgeConditionType;
import com.elingo.gamification.entity.UserBadge;
import com.elingo.gamification.repository.UserBadgeRepository;
import com.elingo.gamification.service.impl.BadgeAwardServiceImpl;
import com.elingo.notification.entity.NotificationType;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class BadgeAwardServiceTest {

    @Mock
    private BadgeCatalog badgeCatalog;

    @Mock
    private UserBadgeRepository userBadgeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private BadgeEvaluator streakEvaluator;

    private BadgeAwardServiceImpl badgeAwardService;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        when(streakEvaluator.supportsType()).thenReturn(BadgeConditionType.STREAK_DAYS);

        badgeAwardService = new BadgeAwardServiceImpl(
                List.of(streakEvaluator),
                badgeCatalog,
                userBadgeRepository,
                userRepository,
                applicationEventPublisher
        );
        badgeAwardService.init();
    }

    @Nested
    @DisplayName("evaluateAndAward - Success cases")
    class EvaluateAndAwardSuccessTests {

        @Test
        @DisplayName("Awards multiple eligible badges in order and publishes NotificationEvent for each")
        void evaluateAndAward_Success_AwardsMultipleBadgesAndPublishesNotifications() {
            Badge badge7 = Badge.builder()
                    .code("STREAK_7")
                    .name("7-Day Streak")
                    .conditionType(BadgeConditionType.STREAK_DAYS)
                    .conditionValue(7)
                    .build();
            badge7.setId(10L);

            Badge badge14 = Badge.builder()
                    .code("STREAK_14")
                    .name("14-Day Streak")
                    .conditionType(BadgeConditionType.STREAK_DAYS)
                    .conditionValue(14)
                    .build();
            badge14.setId(11L);

            Badge badge30 = Badge.builder()
                    .code("STREAK_30")
                    .name("30-Day Streak")
                    .conditionType(BadgeConditionType.STREAK_DAYS)
                    .conditionValue(30)
                    .build();
            badge30.setId(12L);

            User user = User.builder().build();
            user.setId(USER_ID);

            when(badgeCatalog.getBadgesByType(BadgeConditionType.STREAK_DAYS))
                    .thenReturn(List.of(badge7, badge14, badge30));
            when(userBadgeRepository.findEarnedBadgeIdsByUserId(USER_ID))
                    .thenReturn(Set.of());
            // Progress is 20 days: eligible for 7 and 14, but not 30
            when(streakEvaluator.getCurrentProgress(USER_ID)).thenReturn(20L);
            when(streakEvaluator.isEligible(20L, badge7)).thenReturn(true);
            when(streakEvaluator.isEligible(20L, badge14)).thenReturn(true);
            when(streakEvaluator.isEligible(20L, badge30)).thenReturn(false);
            when(userRepository.getReferenceById(USER_ID)).thenReturn(user);

            List<Badge> awarded = badgeAwardService.evaluateAndAward(USER_ID, BadgeConditionType.STREAK_DAYS);

            assertThat(awarded).containsExactly(badge7, badge14);

            // Verify progress was calculated only once
            verify(streakEvaluator, times(1)).getCurrentProgress(USER_ID);

            // Verify batch save
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<UserBadge>> userBadgesCaptor = ArgumentCaptor.forClass(List.class);
            verify(userBadgeRepository).saveAll(userBadgesCaptor.capture());
            List<UserBadge> savedUserBadges = userBadgesCaptor.getValue();
            assertThat(savedUserBadges).hasSize(2);
            assertThat(savedUserBadges.get(0).getBadge()).isEqualTo(badge7);
            assertThat(savedUserBadges.get(1).getBadge()).isEqualTo(badge14);

            // Verify NotificationEvents published
            ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
            verify(applicationEventPublisher, times(2)).publishEvent(eventCaptor.capture());

            List<NotificationEvent> publishedEvents = eventCaptor.getAllValues();
            assertThat(publishedEvents.get(0).type()).isEqualTo(NotificationType.BADGE_EARNED);
            assertThat(publishedEvents.get(0).recipientId()).isEqualTo(USER_ID);
            assertThat(publishedEvents.get(0).refId()).isEqualTo(10L);
            assertThat(publishedEvents.get(0).args()).containsEntry("badgeName", "7-Day Streak");

            assertThat(publishedEvents.get(1).type()).isEqualTo(NotificationType.BADGE_EARNED);
            assertThat(publishedEvents.get(1).recipientId()).isEqualTo(USER_ID);
            assertThat(publishedEvents.get(1).refId()).isEqualTo(11L);
            assertThat(publishedEvents.get(1).args()).containsEntry("badgeName", "14-Day Streak");
        }

        @Test
        @DisplayName("Does not award badges when progress is not eligible for any unearned badge")
        void evaluateAndAward_Success_WhenNotEligible_DoesNothing() {
            Badge badge30 = Badge.builder()
                    .code("STREAK_30")
                    .conditionType(BadgeConditionType.STREAK_DAYS)
                    .conditionValue(30)
                    .build();
            badge30.setId(12L);

            when(badgeCatalog.getBadgesByType(BadgeConditionType.STREAK_DAYS))
                    .thenReturn(List.of(badge30));
            when(userBadgeRepository.findEarnedBadgeIdsByUserId(USER_ID))
                    .thenReturn(Set.of());
            when(streakEvaluator.getCurrentProgress(USER_ID)).thenReturn(10L);
            when(streakEvaluator.isEligible(10L, badge30)).thenReturn(false);

            List<Badge> awarded = badgeAwardService.evaluateAndAward(USER_ID, BadgeConditionType.STREAK_DAYS);

            assertThat(awarded).isEmpty();
            verify(userBadgeRepository, never()).saveAll(any());
            verify(applicationEventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Does nothing when all badges of condition type are already earned")
        void evaluateAndAward_Success_WhenAllBadgesAlreadyEarned() {
            Badge badge7 = Badge.builder()
                    .code("STREAK_7")
                    .conditionType(BadgeConditionType.STREAK_DAYS)
                    .conditionValue(7)
                    .build();
            badge7.setId(10L);

            when(badgeCatalog.getBadgesByType(BadgeConditionType.STREAK_DAYS))
                    .thenReturn(List.of(badge7));
            when(userBadgeRepository.findEarnedBadgeIdsByUserId(USER_ID))
                    .thenReturn(Set.of(10L));

            List<Badge> awarded = badgeAwardService.evaluateAndAward(USER_ID, BadgeConditionType.STREAK_DAYS);

            assertThat(awarded).isEmpty();
            verify(streakEvaluator, never()).getCurrentProgress(anyLong());
            verify(userBadgeRepository, never()).saveAll(any());
            verify(applicationEventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("evaluateAndAward - Edge cases")
    class EvaluateAndAwardEdgeCasesTests {

        @Test
        @DisplayName("Returns empty list when userId is null")
        void evaluateAndAward_WhenUserIdNull_ReturnsEmptyList() {
            List<Badge> awarded = badgeAwardService.evaluateAndAward(null, BadgeConditionType.STREAK_DAYS);

            assertThat(awarded).isEmpty();
            verifyNoInteractions(badgeCatalog);
            verifyNoInteractions(userBadgeRepository);
            verifyNoInteractions(applicationEventPublisher);
        }

        @Test
        @DisplayName("Returns empty list when no evaluator supports the condition type")
        void evaluateAndAward_WhenNoEvaluator_ReturnsEmptyList() {
            List<Badge> awarded = badgeAwardService.evaluateAndAward(USER_ID, BadgeConditionType.WORDS_LEARNED);

            assertThat(awarded).isEmpty();
            verifyNoInteractions(badgeCatalog);
            verifyNoInteractions(userBadgeRepository);
        }

        @Test
        @DisplayName("Returns empty list when catalog has no badges for the condition type")
        void evaluateAndAward_WhenCatalogEmpty_ReturnsEmptyList() {
            when(badgeCatalog.getBadgesByType(BadgeConditionType.STREAK_DAYS)).thenReturn(List.of());

            List<Badge> awarded = badgeAwardService.evaluateAndAward(USER_ID, BadgeConditionType.STREAK_DAYS);

            assertThat(awarded).isEmpty();
            verify(userBadgeRepository, never()).findEarnedBadgeIdsByUserId(anyLong());
        }
    }
}
