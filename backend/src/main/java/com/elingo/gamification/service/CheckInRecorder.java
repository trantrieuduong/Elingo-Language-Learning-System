package com.elingo.gamification.service;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.gamification.config.GamificationProperties;
import com.elingo.gamification.entity.DailyCheckin;
import com.elingo.gamification.entity.LearningStreak;
import com.elingo.gamification.entity.XpSource;
import com.elingo.gamification.repository.DailyCheckInRepository;
import com.elingo.gamification.repository.LearningStreakRepository;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class CheckInRecorder {

    private final LearningStreakRepository streakRepo;
    private final DailyCheckInRepository checkInRepo;
    private final XpService xpService;
    private final GamificationProperties props;
    private final UserRepository userRepository;

    @Transactional
    public void record(long userId, LocalDate date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));
        streakRepo.insertIgnore(userId);
        LearningStreak s = streakRepo.findByIdForUpdate(userId).orElseThrow();

        LocalDate last = s.getLastActiveDate();
        if (last != null && !date.isAfter(last)) return; // đã check-in

        int streak = date.minusDays(1).equals(last) ? s.getCurrentStreak() + 1 : 1;
        s.setCurrentStreak(streak);
        s.setLongestStreak(Math.max(s.getLongestStreak(), streak));
        s.setLastActiveDate(date);

        int reward = props.rule(XpSource.DAILY_CHECKIN).xp();
        checkInRepo.save(DailyCheckin.builder()
                .user(user)
                .checkinDate(date)
                .streakDayNo(streak)
                .xpRewarded(reward)
                .build());

        xpService.addXp(userId, reward, XpSource.DAILY_CHECKIN, null);
    }
}
