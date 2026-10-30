package com.elingo.config;

import com.elingo.gamification.entity.Level;
import com.elingo.gamification.repository.LevelRepository;
import com.elingo.gamification.service.LevelCatalog;
import com.elingo.vocabulary.entity.CefrLevel;
import com.elingo.vocabulary.repository.CefrLevelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "DATABASE-INITIALIZER")
public class DatabaseInitializer implements CommandLineRunner {

    private final CefrLevelRepository cefrLevelRepository;
    private final LevelRepository levelRepository;
    private final LevelCatalog levelCatalog;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking database records for initial seeding...");

        seedCefrLevels();
        seedLevels();

        log.info("Database initialization check finished.");
    }

    private void seedCefrLevels() {
        if (cefrLevelRepository.count() > 0) {
            log.info("Table 'cefr_levels' already contains records. Skipping.");
            return;
        }

        List<CefrLevel> defaultLevels = List.of(
                CefrLevel.builder().label("A1").build(),
                CefrLevel.builder().label("A2").build(),
                CefrLevel.builder().label("B1").build(),
                CefrLevel.builder().label("B2").build(),
                CefrLevel.builder().label("C1").build(),
                CefrLevel.builder().label("C2").build()
        );

        cefrLevelRepository.saveAll(defaultLevels);
        log.info("Seeded {} CEFR levels into table 'cefr_levels'.", defaultLevels.size());
    }

    private void seedLevels() {
        if (levelRepository.count() > 0) {
            log.info("Table 'levels' already contains records. Skipping.");
            return;
        }

        Level defaultLevel = Level.builder()
                .levelNo((short) 1)
                .requiredXp(0)
                .build();

        levelRepository.save(defaultLevel);
        levelCatalog.reload();
        log.info("Seeded 1 gamification levels into table 'levels' and reloaded LevelCatalog.");
    }
}

