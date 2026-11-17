package com.elingo.gamification.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.gamification.catalog.LevelCatalog;
import com.elingo.gamification.dto.response.LevelResponse;
import com.elingo.gamification.dto.response.UserLevelProgressResponse;
import com.elingo.gamification.entity.Level;
import com.elingo.gamification.entity.UserXp;
import com.elingo.gamification.repository.UserXpRepository;
import com.elingo.gamification.service.LevelService;
import com.elingo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "LEVEL-SERVICE")
public class LevelServiceImpl implements LevelService {

    private final LevelCatalog levelCatalog;
    private final UserXpRepository userXpRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LevelResponse> getAllLevels() {
        List<Level> levels = levelCatalog.getAllLevels();
        return levels.stream()
                .map(l -> new LevelResponse(l.getLevelNo(), l.getRequiredXp(), l.getTitle()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserLevelProgressResponse getUserLevelProgress(Long userId) {
        if (userId == null || !userRepository.existsById(userId)) {
            throw new AppException(AppError.USER_NOT_FOUND);
        }

        UserXp userXp = userXpRepository.findById(userId).orElse(null);
        int totalXp = (userXp != null && userXp.getTotalXp() != null) ? userXp.getTotalXp() : 0;

        short currentLevelNo = determineCurrentLevelNo(userXp, totalXp);
        Level currentLevel = levelCatalog.getByLevelNo(currentLevelNo);
        String currentTitle = (currentLevel != null && currentLevel.getTitle() != null)
                ? currentLevel.getTitle()
                : "Level " + currentLevelNo;
        int currentLevelRequiredXp = (currentLevel != null && currentLevel.getRequiredXp() != null)
                ? currentLevel.getRequiredXp()
                : 0;

        Level nextLevel = levelCatalog.getNextLevel(currentLevelNo);
        return buildProgressResponse(userId, currentLevelNo, currentTitle, totalXp, currentLevelRequiredXp, nextLevel);
    }

    private short determineCurrentLevelNo(UserXp userXp, int totalXp) {
        if (userXp != null && userXp.getLevel() != null && userXp.getLevel().getLevelNo() != null) {
            return userXp.getLevel().getLevelNo();
        }
        return (short) levelCatalog.levelFor(totalXp);
    }

    private UserLevelProgressResponse buildProgressResponse(
            Long userId,
            short currentLevelNo,
            String currentTitle,
            int totalXp,
            int currentLevelRequiredXp,
            Level nextLevel) {

        if (nextLevel == null) {
            return new UserLevelProgressResponse(
                    userId,
                    currentLevelNo,
                    currentTitle,
                    totalXp,
                    currentLevelRequiredXp,
                    null,
                    null,
                    null,
                    0,
                    100.0
            );
        }

        short nextLevelNo = nextLevel.getLevelNo();
        String nextLevelTitle = nextLevel.getTitle();
        int nextLevelRequiredXp = (nextLevel.getRequiredXp() != null) ? nextLevel.getRequiredXp() : 0;
        int xpToNextLevel = Math.max(0, nextLevelRequiredXp - totalXp);

        int span = nextLevelRequiredXp - currentLevelRequiredXp;
        int achieved = Math.max(0, totalXp - currentLevelRequiredXp);
        double progressPercent = (span > 0)
                ? Math.min(100.0, Math.round(((double) achieved / span) * 1000.0) / 10.0)
                : 100.0;

        return new UserLevelProgressResponse(
                userId,
                currentLevelNo,
                currentTitle,
                totalXp,
                currentLevelRequiredXp,
                nextLevelNo,
                nextLevelTitle,
                nextLevelRequiredXp,
                xpToNextLevel,
                progressPercent
        );
    }
}
