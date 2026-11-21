package com.elingo.gamification.service;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.gamification.entity.BadgeConditionType;
import com.elingo.gamification.entity.DailyCheckin;
import com.elingo.gamification.entity.LearningStreak;
import com.elingo.gamification.entity.XpSource;
import com.elingo.gamification.publisher.GamificationEventPublisher;
import com.elingo.gamification.repository.DailyCheckInRepository;
import com.elingo.gamification.repository.LearningStreakRepository;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("CheckInRecorder Tests")
class CheckInRecorderTest {

    @Mock
    private LearningStreakRepository streakRepo;

    @Mock
    private DailyCheckInRepository checkInRepo;

    @Mock
    private XpService xpService;

    @Mock
    private XpPolicy xpPolicy;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GamificationEventPublisher gamificationEventPublisher;

    @Mock
    private BadgeAwardService badgeAwardService;

    private CheckInRecorder recorder;

    private static final Long USER_ID = 10L;
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
    private static final int CHECKIN_XP = 5;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(USER_ID)
                .username("test_user")
                .email("test@example.com")
                .build();

        lenient().when(xpPolicy.getXp(XpSource.DAILY_CHECKIN)).thenReturn(CHECKIN_XP);

        recorder = new CheckInRecorder(
                streakRepo,
                checkInRepo,
                xpService,
                xpPolicy,
                userRepository,
                gamificationEventPublisher,
                badgeAwardService
        );
    }

    @Nested
    @DisplayName("record Tests")
    class RecordTests {

        @Test
        @DisplayName("First-time check-in successfully creates streak, awards XP, and triggers badge evaluation")
        void record_FirstTime_Success() {
            LearningStreak streak = LearningStreak.builder()
                    .userId(USER_ID)
                    .currentStreak(0)
                    .longestStreak(0)
                    .lastActiveDate(null)
                    .build();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(streakRepo.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(streak));

            recorder.record(USER_ID, TODAY);

            verify(streakRepo).insertIgnore(USER_ID);
            verify(streakRepo).findByIdForUpdate(USER_ID);

            assertThat(streak.getCurrentStreak()).isEqualTo(1);
            assertThat(streak.getLongestStreak()).isEqualTo(1);
            assertThat(streak.getLastActiveDate()).isEqualTo(TODAY);

            ArgumentCaptor<DailyCheckin> checkInCaptor = ArgumentCaptor.forClass(DailyCheckin.class);
            verify(checkInRepo).save(checkInCaptor.capture());
            DailyCheckin savedCheckIn = checkInCaptor.getValue();
            assertThat(savedCheckIn.getUser()).isEqualTo(testUser);
            assertThat(savedCheckIn.getCheckinDate()).isEqualTo(TODAY);
            assertThat(savedCheckIn.getStreakDayNo()).isEqualTo(1);
            assertThat(savedCheckIn.getXpRewarded()).isEqualTo(CHECKIN_XP);

            verify(xpService).addXp(USER_ID, CHECKIN_XP, XpSource.DAILY_CHECKIN, null);
            verify(gamificationEventPublisher).sendXpGained(USER_ID, CHECKIN_XP);
            verify(badgeAwardService).evaluateAndAward(USER_ID, BadgeConditionType.STREAK_DAYS);
        }

        @Test
        @DisplayName("Consecutive day check-in successfully increments streak and updates longest streak")
        void record_ConsecutiveDay_IncrementsStreak() {
            LearningStreak streak = LearningStreak.builder()
                    .userId(USER_ID)
                    .currentStreak(3)
                    .longestStreak(3)
                    .lastActiveDate(TODAY.minusDays(1))
                    .build();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(streakRepo.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(streak));

            recorder.record(USER_ID, TODAY);

            assertThat(streak.getCurrentStreak()).isEqualTo(4);
            assertThat(streak.getLongestStreak()).isEqualTo(4);
            assertThat(streak.getLastActiveDate()).isEqualTo(TODAY);

            verify(xpService).addXp(USER_ID, CHECKIN_XP, XpSource.DAILY_CHECKIN, null);
            verify(gamificationEventPublisher).sendXpGained(USER_ID, CHECKIN_XP);
            verify(badgeAwardService).evaluateAndAward(USER_ID, BadgeConditionType.STREAK_DAYS);
        }

        @Test
        @DisplayName("Broken streak check-in resets current streak to 1 while retaining longest streak")
        void record_BrokenStreak_ResetsCurrentStreakToOne() {
            LearningStreak streak = LearningStreak.builder()
                    .userId(USER_ID)
                    .currentStreak(7)
                    .longestStreak(10)
                    .lastActiveDate(TODAY.minusDays(3))
                    .build();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(streakRepo.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(streak));

            recorder.record(USER_ID, TODAY);

            assertThat(streak.getCurrentStreak()).isEqualTo(1);
            assertThat(streak.getLongestStreak()).isEqualTo(10);
            assertThat(streak.getLastActiveDate()).isEqualTo(TODAY);

            verify(xpService).addXp(USER_ID, CHECKIN_XP, XpSource.DAILY_CHECKIN, null);
            verify(gamificationEventPublisher).sendXpGained(USER_ID, CHECKIN_XP);
            verify(badgeAwardService).evaluateAndAward(USER_ID, BadgeConditionType.STREAK_DAYS);
        }

        @Test
        @DisplayName("Same-day check-in is safely ignored without awarding additional XP")
        void record_AlreadyCheckedIn_Skips() {
            LearningStreak streak = LearningStreak.builder()
                    .userId(USER_ID)
                    .currentStreak(5)
                    .longestStreak(10)
                    .lastActiveDate(TODAY)
                    .build();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(streakRepo.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(streak));

            recorder.record(USER_ID, TODAY);

            verify(checkInRepo, never()).save(any());
            verify(xpService, never()).addXp(anyLong(), anyInt(), any(), any());
            verify(gamificationEventPublisher, never()).sendXpGained(anyLong(), anyInt());
            verify(badgeAwardService, never()).evaluateAndAward(anyLong(), any());
        }

        @Test
        @DisplayName("Throws AppException when target user does not exist")
        void record_UserNotFound_ThrowsException() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> recorder.record(USER_ID, TODAY))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));

            verify(streakRepo, never()).insertIgnore(anyLong());
            verify(gamificationEventPublisher, never()).sendXpGained(anyLong(), anyInt());
        }
    }
}
