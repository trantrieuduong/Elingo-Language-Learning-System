package com.elingo.gamification.service;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.gamification.dto.response.StreakResponse;
import com.elingo.gamification.entity.LearningStreak;
import com.elingo.gamification.repository.DailyCheckInRepository;
import com.elingo.gamification.repository.LearningStreakRepository;
import com.elingo.gamification.service.impl.StreakServiceImpl;
import com.elingo.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class StreakServiceTest {

    @Mock
    private LearningStreakRepository streakRepo;

    @Mock
    private DailyCheckInRepository checkInRepo;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StreakServiceImpl streakService;

    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("getUserStreak")
    class GetUserStreakTests {
        
        @Test
        @DisplayName("Get streak successfully with default year and month when user checked in today")
        void getUserStreak_Success_WhenUsingDefaultYearAndMonth_CheckedInToday() {
            LocalDate today = LocalDate.now();
            LocalDate startOfMonth = today.withDayOfMonth(1);
            LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);

            LearningStreak streak = LearningStreak.builder()
                    .userId(USER_ID)
                    .currentStreak(5)
                    .longestStreak(10)
                    .lastActiveDate(today)
                    .build();

            List<LocalDate> checkinDates = List.of(
                    today.minusDays(2),
                    today.minusDays(1),
                    today
            );

            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(streakRepo.findById(USER_ID)).thenReturn(Optional.of(streak));
            when(checkInRepo.findCheckinDatesBetween(USER_ID, startOfMonth, endOfMonth)).thenReturn(checkinDates);

            StreakResponse response = streakService.getUserStreak(USER_ID, null, null);

            assertThat(response).isNotNull();
            assertThat(response.userId()).isEqualTo(USER_ID);
            assertThat(response.currentStreak()).isEqualTo(5);
            assertThat(response.longestStreak()).isEqualTo(10);
            assertThat(response.lastActiveDate()).isEqualTo(today);
            assertThat(response.checkedInToday()).isTrue();
            assertThat(response.monthCheckinCount()).isEqualTo(3);
            assertThat(response.checkinDates()).containsExactlyElementsOf(checkinDates);

            verify(userRepository).existsById(USER_ID);
            verify(streakRepo).findById(USER_ID);
            verify(checkInRepo).findCheckinDatesBetween(USER_ID, startOfMonth, endOfMonth);
        }

        @Test
        @DisplayName("Get streak successfully with specified year and month")
        void getUserStreak_Success_WhenCustomYearAndMonthProvided() {
            int year = 2026;
            int month = 10;
            LocalDate startDate = LocalDate.of(2026, 10, 1);
            LocalDate endDate = LocalDate.of(2026, 10, 31);
            LocalDate pastActiveDate = LocalDate.of(2026, 10, 15);

            LearningStreak streak = LearningStreak.builder()
                    .userId(USER_ID)
                    .currentStreak(3)
                    .longestStreak(7)
                    .lastActiveDate(pastActiveDate)
                    .build();

            List<LocalDate> checkinDates = List.of(
                    LocalDate.of(2026, 10, 13),
                    LocalDate.of(2026, 10, 14),
                    LocalDate.of(2026, 10, 15)
            );

            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(streakRepo.findById(USER_ID)).thenReturn(Optional.of(streak));
            when(checkInRepo.findCheckinDatesBetween(USER_ID, startDate, endDate)).thenReturn(checkinDates);

            StreakResponse response = streakService.getUserStreak(USER_ID, year, month);

            assertThat(response).isNotNull();
            assertThat(response.userId()).isEqualTo(USER_ID);
            assertThat(response.currentStreak()).isEqualTo(3);
            assertThat(response.longestStreak()).isEqualTo(7);
            assertThat(response.lastActiveDate()).isEqualTo(pastActiveDate);
            assertThat(response.checkedInToday()).isFalse();
            assertThat(response.monthCheckinCount()).isEqualTo(3);
            assertThat(response.checkinDates()).containsExactlyElementsOf(checkinDates);

            verify(checkInRepo).findCheckinDatesBetween(USER_ID, startDate, endDate);
        }

        @Test
        @DisplayName("Get streak successfully and fallback to current date when year or month is out of valid bounds")
        void getUserStreak_Success_WhenYearOrMonthOutOfRange_FallbacksToCurrentDate() {
            LocalDate today = LocalDate.now();
            LocalDate expectedStartOfMonth = today.withDayOfMonth(1);
            LocalDate expectedEndOfMonth = expectedStartOfMonth.plusMonths(1).minusDays(1);

            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(streakRepo.findById(USER_ID)).thenReturn(Optional.empty());
            when(checkInRepo.findCheckinDatesBetween(USER_ID, expectedStartOfMonth, expectedEndOfMonth)).thenReturn(List.of());

            // year < 2000 and month > 12
            StreakResponse response = streakService.getUserStreak(USER_ID, 1990, 13);

            assertThat(response).isNotNull();
            assertThat(response.userId()).isEqualTo(USER_ID);
            assertThat(response.currentStreak()).isZero();
            assertThat(response.longestStreak()).isZero();
            assertThat(response.lastActiveDate()).isNull();
            assertThat(response.checkedInToday()).isFalse();
            assertThat(response.monthCheckinCount()).isZero();
            assertThat(response.checkinDates()).isEmpty();

            verify(checkInRepo).findCheckinDatesBetween(USER_ID, expectedStartOfMonth, expectedEndOfMonth);
        }

        @Test
        @DisplayName("Get streak successfully when user has no streak record yet, returning zero defaults")
        void getUserStreak_Success_WhenNoStreakRecordExists_ReturnsDefaults() {
            LocalDate today = LocalDate.now();
            LocalDate startOfMonth = today.withDayOfMonth(1);
            LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);

            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(streakRepo.findById(USER_ID)).thenReturn(Optional.empty());
            when(checkInRepo.findCheckinDatesBetween(USER_ID, startOfMonth, endOfMonth)).thenReturn(List.of());

            StreakResponse response = streakService.getUserStreak(USER_ID, null, null);

            assertThat(response).isNotNull();
            assertThat(response.userId()).isEqualTo(USER_ID);
            assertThat(response.currentStreak()).isZero();
            assertThat(response.longestStreak()).isZero();
            assertThat(response.lastActiveDate()).isNull();
            assertThat(response.checkedInToday()).isFalse();
            assertThat(response.monthCheckinCount()).isZero();
            assertThat(response.checkinDates()).isEmpty();
        }

        @Test
        @DisplayName("Get streak successfully with lastActiveDate in past, checkedInToday is false")
        void getUserStreak_Success_WhenLastActiveDateIsYesterday_CheckedInTodayIsFalse() {
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);
            LocalDate startOfMonth = today.withDayOfMonth(1);
            LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);

            LearningStreak streak = LearningStreak.builder()
                    .userId(USER_ID)
                    .currentStreak(2)
                    .longestStreak(4)
                    .lastActiveDate(yesterday)
                    .build();

            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(streakRepo.findById(USER_ID)).thenReturn(Optional.of(streak));
            when(checkInRepo.findCheckinDatesBetween(USER_ID, startOfMonth, endOfMonth)).thenReturn(List.of(yesterday));

            StreakResponse response = streakService.getUserStreak(USER_ID, null, null);

            assertThat(response).isNotNull();
            assertThat(response.checkedInToday()).isFalse();
            assertThat(response.currentStreak()).isEqualTo(2);
            assertThat(response.lastActiveDate()).isEqualTo(yesterday);
        }

        @Test
        @DisplayName("Throws AppException when userId is null")
        void getUserStreak_Fail_WhenUserIdIsNull_ThrowsAppException() {
            assertThatThrownBy(() -> streakService.getUserStreak(null, 2026, 10))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));

            verify(userRepository, never()).existsById(any());
            verify(streakRepo, never()).findById(anyLong());
            verify(checkInRepo, never()).findCheckinDatesBetween(anyLong(), any(), any());
        }

        @Test
        @DisplayName("Throws AppException when user does not exist in repository")
        void getUserStreak_Fail_WhenUserNotFoundInDatabase_ThrowsAppException() {
            Long nonExistentUserId = 999L;
            when(userRepository.existsById(nonExistentUserId)).thenReturn(false);

            assertThatThrownBy(() -> streakService.getUserStreak(nonExistentUserId, 2026, 10))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));

            verify(userRepository).existsById(nonExistentUserId);
            verify(streakRepo, never()).findById(anyLong());
            verify(checkInRepo, never()).findCheckinDatesBetween(anyLong(), any(), any());
        }
    }
}
