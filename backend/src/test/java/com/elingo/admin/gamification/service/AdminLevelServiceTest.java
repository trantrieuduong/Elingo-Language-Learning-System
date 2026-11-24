package com.elingo.admin.gamification.service;

import com.elingo.gamification.dto.request.LevelRequest;
import com.elingo.gamification.dto.response.AdminLevelResponse;
import com.elingo.admin.gamification.service.impl.AdminLevelServiceImpl;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.gamification.service.LevelService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("AdminLevelService Tests")
class AdminLevelServiceTest {

    @Mock
    private LevelService levelService;

    @InjectMocks
    private AdminLevelServiceImpl adminLevelService;

    private static final Short LEVEL_NO = 2;
    private static final AdminLevelResponse SAMPLE_RESPONSE = new AdminLevelResponse(
            (short) 2, 100, "Apprentice", LocalDateTime.now(), LocalDateTime.now()
    );

    @Nested
    @DisplayName("getAllLevels Tests")
    class GetAllLevelsTests {

        @Test
        @DisplayName("Should delegate to levelService.getAllLevelsForAdmin")
        void getAllLevels_Success() {
            when(levelService.getAllLevelsForAdmin()).thenReturn(List.of(SAMPLE_RESPONSE));

            List<AdminLevelResponse> result = adminLevelService.getAllLevels();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).levelNo()).isEqualTo((short) 2);
            verify(levelService).getAllLevelsForAdmin();
        }

        @Test
        @DisplayName("Should return empty list when no levels are present")
        void getAllLevels_EmptyList_Success() {
            when(levelService.getAllLevelsForAdmin()).thenReturn(List.of());

            List<AdminLevelResponse> result = adminLevelService.getAllLevels();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getLevelByNo Tests")
    class GetLevelByNoTests {

        @Test
        @DisplayName("Should return level response when level exists")
        void getLevelByNo_Success() {
            when(levelService.getLevelByNo(LEVEL_NO)).thenReturn(SAMPLE_RESPONSE);

            AdminLevelResponse result = adminLevelService.getLevelByNo(LEVEL_NO);

            assertThat(result).isEqualTo(SAMPLE_RESPONSE);
            verify(levelService).getLevelByNo(LEVEL_NO);
        }

        @Test
        @DisplayName("Should propagate exception when levelService throws LEVEL_NOT_FOUND")
        void getLevelByNo_NotFound_ThrowsException() {
            when(levelService.getLevelByNo((short) 999))
                    .thenThrow(new AppException(AppError.LEVEL_NOT_FOUND));

            assertThatThrownBy(() -> adminLevelService.getLevelByNo((short) 999))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.LEVEL_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("createLevel Tests")
    class CreateLevelTests {

        @Test
        @DisplayName("Should delegate createLevel to levelService and return response")
        void createLevel_Success() {
            LevelRequest request = new LevelRequest(100, "Apprentice");
            when(levelService.createLevel(request)).thenReturn(SAMPLE_RESPONSE);

            AdminLevelResponse result = adminLevelService.createLevel(request);

            assertThat(result.levelNo()).isEqualTo((short) 2);
            assertThat(result.requiredXp()).isEqualTo(100);
            verify(levelService).createLevel(request);
        }

        @Test
        @DisplayName("Should propagate exception when levelService fails")
        void createLevel_ThrowsException() {
            LevelRequest request = new LevelRequest(50, "Invalid");
            when(levelService.createLevel(request))
                    .thenThrow(new AppException(AppError.LEVEL_REQUIRED_XP_INVALID));

            assertThatThrownBy(() -> adminLevelService.createLevel(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.LEVEL_REQUIRED_XP_INVALID));
        }
    }

    @Nested
    @DisplayName("updateLevel Tests")
    class UpdateLevelTests {

        @Test
        @DisplayName("Should delegate updateLevel to levelService and return response")
        void updateLevel_Success() {
            LevelRequest request = new LevelRequest(150, "Updated");
            AdminLevelResponse updatedRes = new AdminLevelResponse(
                    (short) 2, 150, "Updated", LocalDateTime.now(), LocalDateTime.now()
            );

            when(levelService.updateLevel(LEVEL_NO, request)).thenReturn(updatedRes);

            AdminLevelResponse result = adminLevelService.updateLevel(LEVEL_NO, request);

            assertThat(result.requiredXp()).isEqualTo(150);
            verify(levelService).updateLevel(LEVEL_NO, request);
        }

        @Test
        @DisplayName("Should propagate exception when updating level 1")
        void updateLevel_LevelOne_ThrowsException() {
            LevelRequest request = new LevelRequest(0, "Invalid");
            when(levelService.updateLevel((short) 1, request))
                    .thenThrow(new AppException(AppError.DEFAULT_LEVEL_CANNOT_BE_MODIFIED));

            assertThatThrownBy(() -> adminLevelService.updateLevel((short) 1, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.DEFAULT_LEVEL_CANNOT_BE_MODIFIED));
        }
    }

    @Nested
    @DisplayName("deleteLevel Tests")
    class DeleteLevelTests {

        @Test
        @DisplayName("Should delegate deleteLevel to levelService")
        void deleteLevel_Success() {
            adminLevelService.deleteLevel(LEVEL_NO);

            verify(levelService).deleteLevel(LEVEL_NO);
        }

        @Test
        @DisplayName("Should propagate exception when deleting level fails")
        void deleteLevel_ThrowsException() {
            doThrow(new AppException(AppError.LEVEL_HAS_ACTIVE_USERS))
                    .when(levelService).deleteLevel(LEVEL_NO);

            assertThatThrownBy(() -> adminLevelService.deleteLevel(LEVEL_NO))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.LEVEL_HAS_ACTIVE_USERS));
        }
    }
}
