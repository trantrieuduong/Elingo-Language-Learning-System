package com.elingo.gamification.catalog;

import com.elingo.gamification.entity.Level;
import com.elingo.gamification.repository.LevelRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "LEVEL-CATALOG")
public class LevelCatalog {

    private final LevelRepository levelRepository;
    @SuppressWarnings("java:S3077") // List chỉ được gán lại reference mới khi reload()
    private volatile List<Level> levels = List.of();

    @PostConstruct
    public void reload() { // gọi lại khi admin sửa bảng levels
        levels = levelRepository.findAll(Sort.by("requiredXp"));
        log.info("Level catalog loaded size={}", levels.size());
    }

    public int levelFor(long totalXp) {
        int result = 1;
        for (Level l : levels) {
            if (totalXp >= l.getRequiredXp()) result = l.getLevelNo();
            else break;
        }
        return result;
    }

    public List<Level> getAllLevels() {
        return levels;
    }

    public Level getByLevelNo(int levelNo) {
        for (Level l : levels) {
            if (l.getLevelNo() != null && l.getLevelNo() == levelNo) {
                return l;
            }
        }
        return null;
    }

    public Level getNextLevel(int currentLevelNo) {
        for (Level l : levels) {
            if (l.getLevelNo() != null && l.getLevelNo() > currentLevelNo) {
                return l;
            }
        }
        return null;
    }

    public String getTitleForLevel(int levelNo) {
        Level level = getByLevelNo(levelNo);
        if (level != null && level.getTitle() != null && !level.getTitle().isBlank()) {
            return level.getTitle();
        }
        return "Level " + levelNo;
    }
}
