package com.elingo.gamification.catalog;

import com.elingo.gamification.entity.Level;
import com.elingo.gamification.repository.LevelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class LevelCatalogTest {

    @Mock
    private LevelRepository levelRepository;

    @InjectMocks
    private LevelCatalog levelCatalog;

    @Nested
    @DisplayName("reload & levelFor")
    class CatalogTests {

        @Test
        @DisplayName("Calculates correct level based on total XP")
        void levelFor_Success() {
            Level level1 = Level.builder().levelNo((short) 1).requiredXp(0).title("Novice").build();
            Level level2 = Level.builder().levelNo((short) 2).requiredXp(100).title("Apprentice").build();
            Level level3 = Level.builder().levelNo((short) 3).requiredXp(300).title("Expert").build();

            when(levelRepository.findAll(any(Sort.class))).thenReturn(List.of(level1, level2, level3));

            levelCatalog.reload();

            assertThat(levelCatalog.levelFor(0)).isEqualTo(1);
            assertThat(levelCatalog.levelFor(50)).isEqualTo(1);
            assertThat(levelCatalog.levelFor(100)).isEqualTo(2);
            assertThat(levelCatalog.levelFor(250)).isEqualTo(2);
            assertThat(levelCatalog.levelFor(300)).isEqualTo(3);
            assertThat(levelCatalog.levelFor(1000)).isEqualTo(3);

            verify(levelRepository).findAll(any(Sort.class));
        }

        @Test
        @DisplayName("Defaults to level 1 when catalog is empty")
        void levelFor_EmptyCatalog_ReturnsLevelOne() {
            when(levelRepository.findAll(any(Sort.class))).thenReturn(List.of());

            levelCatalog.reload();

            assertThat(levelCatalog.levelFor(500)).isEqualTo(1);
        }
    }
}
