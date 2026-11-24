package com.elingo.gamification.service.impl;

import com.elingo.gamification.dto.request.LevelRequest;
import com.elingo.gamification.dto.response.AdminLevelResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.gamification.catalog.LevelCatalog;
import com.elingo.gamification.dto.response.LevelResponse;
import com.elingo.gamification.dto.response.UserLevelProgressResponse;
import com.elingo.gamification.entity.Level;
import com.elingo.gamification.entity.UserXp;
import com.elingo.gamification.mapper.LevelMapper;
import com.elingo.gamification.repository.LevelRepository;
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
    private final LevelRepository levelRepository;
    private final UserXpRepository userXpRepository;
    private final UserRepository userRepository;
    private final LevelMapper levelMapper;

    @Override
    @Transactional(readOnly = true)
    public List<LevelResponse> getAllLevels() {
        return levelMapper.toLevelResponseList(levelCatalog.getAllLevels());
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

    @Override
    @Transactional(readOnly = true)
    public List<AdminLevelResponse> getAllLevelsForAdmin() {
        return levelMapper.toAdminLevelResponseList(levelRepository.findAllByOrderByLevelNoAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminLevelResponse getLevelByNo(Short levelNo) {
        return levelMapper.toAdminLevelResponse(levelRepository.findById(levelNo)
                .orElseThrow(() -> new AppException(AppError.LEVEL_NOT_FOUND)));
    }

    @Override
    @Transactional
    public AdminLevelResponse createLevel(LevelRequest request) {
        if (request.requiredXp() <= 0) {
            throw new AppException(AppError.LEVEL_REQUIRED_XP_INVALID);
        }

        List<Level> existing = levelRepository.findAllByOrderByLevelNoAsc();
        short nextLevelNo;
        if (existing.isEmpty()) {
            nextLevelNo = 1;
        } else {
            Level last = existing.getLast();
            if (request.requiredXp() <= last.getRequiredXp()) {
                throw new AppException(AppError.LEVEL_REQUIRED_XP_INVALID);
            }
            nextLevelNo = (short) (last.getLevelNo() + 1);
        }

        Level level = levelMapper.toLevel(request);
        level.setLevelNo(nextLevelNo);

        Level saved = levelRepository.save(level);
        levelCatalog.reload();
        log.info("Level created levelNo={} requiredXp={} title={}", nextLevelNo, saved.getRequiredXp(), saved.getTitle());
        return levelMapper.toAdminLevelResponse(saved);
    }

    @Override
    @Transactional
    public AdminLevelResponse updateLevel(Short levelNo, LevelRequest request) {
        Level level = levelRepository.findById(levelNo)
                .orElseThrow(() -> new AppException(AppError.LEVEL_NOT_FOUND));

        if (levelNo == 1) {
            throw new AppException(AppError.DEFAULT_LEVEL_CANNOT_BE_MODIFIED);
        }

        if (request.requiredXp() <= 0) {
            throw new AppException(AppError.LEVEL_REQUIRED_XP_INVALID);
        }
        validateLevelProgression(levelNo, request.requiredXp());

        levelMapper.updateLevel(level, request);

        Level updated = levelRepository.save(level);
        levelCatalog.reload();
        log.info("Level updated levelNo={} requiredXp={} title={}", levelNo, updated.getRequiredXp(), updated.getTitle());
        return levelMapper.toAdminLevelResponse(updated);
    }

    @Override
    @Transactional
    public void deleteLevel(Short levelNo) {
        if (levelNo == null || levelNo == 1) {
            throw new AppException(AppError.DEFAULT_LEVEL_CANNOT_BE_MODIFIED);
        }

        Level level = levelRepository.findById(levelNo)
                .orElseThrow(() -> new AppException(AppError.LEVEL_NOT_FOUND));

        if (userXpRepository.existsByLevelLevelNo(levelNo)) {
            throw new AppException(AppError.LEVEL_HAS_ACTIVE_USERS);
        }

        levelRepository.delete(level);
        levelCatalog.reload();
        log.info("Level deleted levelNo={}", levelNo);
    }

    private void validateLevelProgression(Short targetLevelNo, int requiredXp) {
        List<Level> existing = levelRepository.findAllByOrderByLevelNoAsc();
        for (Level l : existing) {
            if (l.getLevelNo().equals(targetLevelNo)) continue;

            if (l.getLevelNo() < targetLevelNo && l.getRequiredXp() >= requiredXp) {
                throw new AppException(AppError.LEVEL_REQUIRED_XP_INVALID);
            }
            if (l.getLevelNo() > targetLevelNo && l.getRequiredXp() <= requiredXp) {
                throw new AppException(AppError.LEVEL_REQUIRED_XP_INVALID);
            }
        }
    }
}
