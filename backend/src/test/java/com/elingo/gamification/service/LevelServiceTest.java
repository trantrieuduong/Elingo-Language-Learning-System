package com.elingo.gamification.service;

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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("LevelService Tests")
class LevelServiceTest {

    @Mock
    private LevelCatalog levelCatalog;

    @Mock
    private LevelRepository levelRepository;

    @Mock
    private UserXpRepository userXpRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LevelMapper levelMapper;

    @InjectMocks
    private LevelServiceImpl levelService;

    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("getAllLevels Tests")
    class GetAllLevelsTests {

        @Test
        @DisplayName("Should return all configured levels successfully from catalog")
        void getAllLevels_Success() {
            Level level1 = Level.builder().levelNo((short) 1).requiredXp(0).title("Novice").build();
            Level level2 = Level.builder().levelNo((short) 2).requiredXp(100).title("Explorer").build();

            when(levelCatalog.getAllLevels()).thenReturn(List.of(level1, level2));
            when(levelMapper.toLevelResponseList(List.of(level1, level2))).thenReturn(List.of(
                    new LevelResponse((short) 1, 0, "Novice"),
                    new LevelResponse((short) 2, 100, "Explorer")
            ));

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
            when(levelMapper.toLevelResponseList(List.of())).thenReturn(List.of());

            List<LevelResponse> result = levelService.getAllLevels();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUserLevelProgress Tests")
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
            assertThat(result.xpToNextLevel()).isEqualTo(150);
            assertThat(result.progressPercent()).isEqualTo(25.0);
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
        @DisplayName("Should throw USER_NOT_FOUND exception when user ID is null")
        void getUserLevelProgress_Fail_UserIdNull() {
            assertThatThrownBy(() -> levelService.getUserLevelProgress(null))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("Admin Level CRUD Operations")
    class AdminLevelCrudTests {

        @Test
        @DisplayName("getAllLevelsForAdmin returns list of mapped AdminLevelResponse")
        void getAllLevelsForAdmin_Success() {
            Level level1 = Level.builder().levelNo((short) 1).requiredXp(0).title("Novice").build();
            AdminLevelResponse res1 = new AdminLevelResponse((short) 1, 0, "Novice", LocalDateTime.now(), LocalDateTime.now());

            when(levelRepository.findAllByOrderByLevelNoAsc()).thenReturn(List.of(level1));
            when(levelMapper.toAdminLevelResponseList(List.of(level1))).thenReturn(List.of(res1));

            List<AdminLevelResponse> result = levelService.getAllLevelsForAdmin();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).levelNo()).isEqualTo((short) 1);
        }

        @Test
        @DisplayName("getLevelByNo returns mapped AdminLevelResponse when found")
        void getLevelByNo_Success() {
            Level level2 = Level.builder().levelNo((short) 2).requiredXp(100).title("Explorer").build();
            AdminLevelResponse res2 = new AdminLevelResponse((short) 2, 100, "Explorer", LocalDateTime.now(), LocalDateTime.now());

            when(levelRepository.findById((short) 2)).thenReturn(Optional.of(level2));
            when(levelMapper.toAdminLevelResponse(level2)).thenReturn(res2);

            AdminLevelResponse result = levelService.getLevelByNo((short) 2);

            assertThat(result.levelNo()).isEqualTo((short) 2);
            assertThat(result.requiredXp()).isEqualTo(100);
        }

        @Test
        @DisplayName("createLevel succeeds with valid requiredXp and increments levelNo")
        void createLevel_Success() {
            Level existingLevel1 = Level.builder().levelNo((short) 1).requiredXp(0).build();
            when(levelRepository.findAllByOrderByLevelNoAsc()).thenReturn(List.of(existingLevel1));

            LevelRequest req = new LevelRequest(100, "Apprentice");
            Level levelToSave = Level.builder().requiredXp(100).title("Apprentice").build();
            Level saved = Level.builder().levelNo((short) 2).requiredXp(100).title("Apprentice").build();
            AdminLevelResponse res = new AdminLevelResponse((short) 2, 100, "Apprentice", LocalDateTime.now(), LocalDateTime.now());

            when(levelMapper.toLevel(req)).thenReturn(levelToSave);
            when(levelRepository.save(any(Level.class))).thenReturn(saved);
            when(levelMapper.toAdminLevelResponse(saved)).thenReturn(res);

            AdminLevelResponse created = levelService.createLevel(req);

            assertThat(created.levelNo()).isEqualTo((short) 2);
            assertThat(created.requiredXp()).isEqualTo(100);
            verify(levelCatalog).reload();
        }

        @Test
        @DisplayName("createLevel fails when required XP is not greater than previous level")
        void createLevel_Fail_RequiredXpNotIncreasing() {
            Level existingLevel1 = Level.builder().levelNo((short) 1).requiredXp(100).build();
            when(levelRepository.findAllByOrderByLevelNoAsc()).thenReturn(List.of(existingLevel1));

            LevelRequest req = new LevelRequest(50, "Invalid");

            assertThatThrownBy(() -> levelService.createLevel(req))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.LEVEL_REQUIRED_XP_INVALID));
        }

        @Test
        @DisplayName("updateLevel fails when modifying default level 1")
        void updateLevel_Fail_LevelOneCannotBeModified() {
            Level level1 = Level.builder().levelNo((short) 1).requiredXp(0).title("Default").build();
            when(levelRepository.findById((short) 1)).thenReturn(Optional.of(level1));

            LevelRequest req = new LevelRequest(0, "Title");

            assertThatThrownBy(() -> levelService.updateLevel((short) 1, req))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DEFAULT_LEVEL_CANNOT_BE_MODIFIED));
        }

        @Test
        @DisplayName("updateLevel succeeds for level > 1 (PUT: full update)")
        void updateLevel_Success() {
            Level level2 = Level.builder().levelNo((short) 2).requiredXp(100).title("Novice").build();
            Level level1 = Level.builder().levelNo((short) 1).requiredXp(0).title("Default").build();
            when(levelRepository.findById((short) 2)).thenReturn(Optional.of(level2));
            when(levelRepository.findAllByOrderByLevelNoAsc()).thenReturn(List.of(level1, level2));
            when(levelRepository.save(any(Level.class))).thenReturn(level2);

            AdminLevelResponse res = new AdminLevelResponse((short) 2, 150, "Updated", LocalDateTime.now(), LocalDateTime.now());
            when(levelMapper.toAdminLevelResponse(level2)).thenReturn(res);

            LevelRequest req = new LevelRequest(150, "Updated");
            AdminLevelResponse updated = levelService.updateLevel((short) 2, req);

            assertThat(updated.requiredXp()).isEqualTo(150);
            verify(levelMapper).updateLevel(level2, req);
            verify(levelCatalog).reload();
        }

        @Test
        @DisplayName("deleteLevel fails when deleting level 1")
        void deleteLevel_Fail_LevelOneCannotBeDeleted() {
            assertThatThrownBy(() -> levelService.deleteLevel((short) 1))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DEFAULT_LEVEL_CANNOT_BE_MODIFIED));
        }

        @Test
        @DisplayName("deleteLevel fails when level has active users")
        void deleteLevel_Fail_LevelHasActiveUsers() {
            Level level2 = Level.builder().levelNo((short) 2).requiredXp(100).build();
            when(levelRepository.findById((short) 2)).thenReturn(Optional.of(level2));
            when(userXpRepository.existsByLevelLevelNo((short) 2)).thenReturn(true);

            assertThatThrownBy(() -> levelService.deleteLevel((short) 2))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.LEVEL_HAS_ACTIVE_USERS));
        }

        @Test
        @DisplayName("deleteLevel succeeds when level has no users")
        void deleteLevel_Success() {
            Level level2 = Level.builder().levelNo((short) 2).requiredXp(100).build();
            when(levelRepository.findById((short) 2)).thenReturn(Optional.of(level2));
            when(userXpRepository.existsByLevelLevelNo((short) 2)).thenReturn(false);

            levelService.deleteLevel((short) 2);

            verify(levelRepository).delete(level2);
            verify(levelCatalog).reload();
        }
    }
}
