package com.elingo.gamification.service;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.gamification.catalog.LevelCatalog;
import com.elingo.gamification.dto.response.LevelResponse;
import com.elingo.gamification.dto.response.UserLevelProgressResponse;
import com.elingo.gamification.entity.Level;
import com.elingo.gamification.entity.UserXp;
import com.elingo.gamification.repository.UserXpRepository;
import com.elingo.gamification.service.impl.LevelServiceImpl;
import com.elingo.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("LevelService Tests")
class LevelServiceTest {

    @Mock
    private LevelCatalog levelCatalog;

    @Mock
    private UserXpRepository userXpRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LevelServiceImpl levelService;

    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("getAllLevels")
    class GetAllLevelsTests {

        @Test
        @DisplayName("Should return all configured levels successfully from catalog")
        void getAllLevels_Success() {
            Level level1 = Level.builder().levelNo((short) 1).requiredXp(0).title("Novice").build();
            Level level2 = Level.builder().levelNo((short) 2).requiredXp(100).title("Explorer").build();

            when(levelCatalog.getAllLevels()).thenReturn(List.of(level1, level2));

            List<LevelResponse> result = levelService.getAllLevels();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).levelNo()).isEqualTo((short) 1);
            assertThat(result.get(0).title()).isEqualTo("Novice");
            assertThat(result.get(1).levelNo()).isEqualTo((short) 2);
            assertThat(result.get(1).requiredXp()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should return empty list when catalog has no levels")
        void getAllLevels_EmptyCatalog_ReturnsEmptyList() {
            when(levelCatalog.getAllLevels()).thenReturn(List.of());

            List<LevelResponse> result = levelService.getAllLevels();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUserLevelProgress")
    class GetUserLevelProgressTests {

        @Test
        @DisplayName("Should calculate user level progress accurately between levels")
        void getUserLevelProgress_Success_NormalProgress() {
            Level currentLevel = Level.builder().levelNo((short) 2).requiredXp(100).title("Explorer").build();
            Level nextLevel = Level.builder().levelNo((short) 3).requiredXp(300).title("Challenger").build();

            UserXp userXp = UserXp.builder()
                    .userId(USER_ID)
                    .level(currentLevel)
                    .totalXp(150)
                    .build();

            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(userXpRepository.findById(USER_ID)).thenReturn(Optional.of(userXp));
            when(levelCatalog.getByLevelNo(2)).thenReturn(currentLevel);
            when(levelCatalog.getNextLevel(2)).thenReturn(nextLevel);

            UserLevelProgressResponse result = levelService.getUserLevelProgress(USER_ID);

            assertThat(result.userId()).isEqualTo(USER_ID);
            assertThat(result.currentLevel()).isEqualTo((short) 2);
            assertThat(result.currentTitle()).isEqualTo("Explorer");
            assertThat(result.totalXp()).isEqualTo(150);
            assertThat(result.currentLevelRequiredXp()).isEqualTo(100);
            assertThat(result.nextLevel()).isEqualTo((short) 3);
            assertThat(result.nextLevelTitle()).isEqualTo("Challenger");
            assertThat(result.nextLevelRequiredXp()).isEqualTo(300);
            assertThat(result.xpToNextLevel()).isEqualTo(150); // 300 - 150 = 150
            assertThat(result.progressPercent()).isEqualTo(25.0); // (150 - 100) / (300 - 100) = 50 / 200 = 25.0%
        }

        @Test
        @DisplayName("Should handle max level correctly when next level is null")
        void getUserLevelProgress_Success_MaxLevel() {
            Level maxLevel = Level.builder().levelNo((short) 50).requiredXp(50000).title("Grandmaster").build();

            UserXp userXp = UserXp.builder()
                    .userId(USER_ID)
                    .level(maxLevel)
                    .totalXp(60000)
                    .build();

            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(userXpRepository.findById(USER_ID)).thenReturn(Optional.of(userXp));
            when(levelCatalog.getByLevelNo(50)).thenReturn(maxLevel);
            when(levelCatalog.getNextLevel(50)).thenReturn(null);

            UserLevelProgressResponse result = levelService.getUserLevelProgress(USER_ID);

            assertThat(result.userId()).isEqualTo(USER_ID);
            assertThat(result.currentLevel()).isEqualTo((short) 50);
            assertThat(result.currentTitle()).isEqualTo("Grandmaster");
            assertThat(result.totalXp()).isEqualTo(60000);
            assertThat(result.nextLevel()).isNull();
            assertThat(result.nextLevelTitle()).isNull();
            assertThat(result.nextLevelRequiredXp()).isNull();
            assertThat(result.xpToNextLevel()).isZero();
            assertThat(result.progressPercent()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should initialize default level 1 progress for new user without user_xp entry")
        void getUserLevelProgress_Success_NewUserWithoutXpRecord() {
            Level level1 = Level.builder().levelNo((short) 1).requiredXp(0).title("Novice").build();
            Level level2 = Level.builder().levelNo((short) 2).requiredXp(100).title("Explorer").build();

            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(userXpRepository.findById(USER_ID)).thenReturn(Optional.empty());
            when(levelCatalog.levelFor(0)).thenReturn(1);
            when(levelCatalog.getByLevelNo(1)).thenReturn(level1);
            when(levelCatalog.getNextLevel(1)).thenReturn(level2);

            UserLevelProgressResponse result = levelService.getUserLevelProgress(USER_ID);

            assertThat(result.userId()).isEqualTo(USER_ID);
            assertThat(result.currentLevel()).isEqualTo((short) 1);
            assertThat(result.currentTitle()).isEqualTo("Novice");
            assertThat(result.totalXp()).isZero();
            assertThat(result.currentLevelRequiredXp()).isZero();
            assertThat(result.nextLevel()).isEqualTo((short) 2);
            assertThat(result.xpToNextLevel()).isEqualTo(100);
            assertThat(result.progressPercent()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should throw USER_NOT_FOUND exception when user ID does not exist")
        void getUserLevelProgress_Fail_UserNotFound() {
            when(userRepository.existsById(USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> levelService.getUserLevelProgress(USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("Should throw USER_NOT_FOUND exception when user ID is null")
        void getUserLevelProgress_Fail_UserIdNull() {
            assertThatThrownBy(() -> levelService.getUserLevelProgress(null))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));
        }
    }
}
