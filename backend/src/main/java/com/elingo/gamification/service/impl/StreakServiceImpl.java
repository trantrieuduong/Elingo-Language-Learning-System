package com.elingo.gamification.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.gamification.dto.response.StreakResponse;
import com.elingo.gamification.entity.LearningStreak;
import com.elingo.gamification.repository.DailyCheckInRepository;
import com.elingo.gamification.repository.LearningStreakRepository;
import com.elingo.gamification.service.StreakService;
import com.elingo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "STREAK-SERVICE")
public class StreakServiceImpl implements StreakService {

    private final LearningStreakRepository streakRepo;
    private final DailyCheckInRepository checkInRepo;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public StreakResponse getUserStreak(Long userId, Integer year, Integer month) {
        if (userId == null || !userRepository.existsById(userId)) {
            throw new AppException(AppError.USER_NOT_FOUND);
        }

        LocalDate today = LocalDate.now();
        int targetYear = (year != null && year >= 2000 && year <= 2100) ? year : today.getYear();
        int targetMonth = (month != null && month >= 1 && month <= 12) ? month : today.getMonthValue();

        LocalDate startDate = LocalDate.of(targetYear, targetMonth, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        Optional<LearningStreak> streakOpt = streakRepo.findById(userId);
        int currentStreak = streakOpt.map(LearningStreak::getCurrentStreak).orElse(0);
        int longestStreak = streakOpt.map(LearningStreak::getLongestStreak).orElse(0);
        LocalDate lastActiveDate = streakOpt.map(LearningStreak::getLastActiveDate).orElse(null);

        boolean checkedInToday = lastActiveDate != null && lastActiveDate.equals(today);
        List<LocalDate> checkinDates = checkInRepo.findCheckinDatesBetween(userId, startDate, endDate);

        return new StreakResponse(
                userId,
                currentStreak,
                longestStreak,
                lastActiveDate,
                checkedInToday,
                checkinDates.size(),
                checkinDates
        );
    }
}
