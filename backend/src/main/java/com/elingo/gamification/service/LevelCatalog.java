package com.elingo.gamification.service;

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
}
