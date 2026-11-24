package com.elingo.admin.gamification.controller;

import com.elingo.gamification.dto.request.LevelRequest;
import com.elingo.gamification.dto.response.AdminLevelResponse;
import com.elingo.admin.gamification.service.AdminLevelService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
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
@DisplayName("AdminLevelController Tests")
class AdminLevelControllerTest {

    @Mock
    private AdminLevelService adminLevelService;

    @InjectMocks
    private AdminLevelController controller;

    private static final Short LEVEL_NO = 2;
    private static final AdminLevelResponse SAMPLE_RESPONSE = new AdminLevelResponse(
            (short) 2, 100, "Apprentice", LocalDateTime.now(), LocalDateTime.now()
    );

    @Nested
    @DisplayName("getAllLevels Tests")
    class GetAllLevelsTests {

        @Test
        @DisplayName("Should return 200 with list of levels when successful")
        void getAllLevels_Success() {
            when(adminLevelService.getAllLevels()).thenReturn(List.of(SAMPLE_RESPONSE));

            ApiResponse<List<AdminLevelResponse>> response = controller.getAllLevels();

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).hasSize(1);
            assertThat(response.getData().get(0).levelNo()).isEqualTo((short) 2);
            verify(adminLevelService).getAllLevels();
        }

        @Test
        @DisplayName("Should return 200 with empty list when no levels exist")
        void getAllLevels_EmptyList_Success() {
            when(adminLevelService.getAllLevels()).thenReturn(List.of());

            ApiResponse<List<AdminLevelResponse>> response = controller.getAllLevels();

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEmpty();
            verify(adminLevelService).getAllLevels();
        }
    }

    @Nested
    @DisplayName("getLevelByNo Tests")
    class GetLevelByNoTests {

        @Test
        @DisplayName("Should return level details when level exists")
        void getLevelByNo_Success() {
            when(adminLevelService.getLevelByNo(LEVEL_NO)).thenReturn(SAMPLE_RESPONSE);

            ApiResponse<AdminLevelResponse> response = controller.getLevelByNo(LEVEL_NO);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().levelNo()).isEqualTo(LEVEL_NO);
            verify(adminLevelService).getLevelByNo(LEVEL_NO);
        }

        @Test
        @DisplayName("Should throw LEVEL_NOT_FOUND when level does not exist")
        void getLevelByNo_NotFound_ThrowsException() {
            when(adminLevelService.getLevelByNo((short) 999))
                    .thenThrow(new AppException(AppError.LEVEL_NOT_FOUND));

            assertThatThrownBy(() -> controller.getLevelByNo((short) 999))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.LEVEL_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("createLevel Tests")
    class CreateLevelTests {

        @Test
        @DisplayName("Should create level successfully when request is valid")
        void createLevel_Success() {
            LevelRequest request = new LevelRequest(100, "Apprentice");
            when(adminLevelService.createLevel(request)).thenReturn(SAMPLE_RESPONSE);

            ApiResponse<AdminLevelResponse> response = controller.createLevel(request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().levelNo()).isEqualTo((short) 2);
            verify(adminLevelService).createLevel(request);
        }

        @Test
        @DisplayName("Should throw LEVEL_REQUIRED_XP_INVALID when required XP is not greater than previous level")
        void createLevel_InvalidXp_ThrowsException() {
            LevelRequest request = new LevelRequest(50, "Invalid");
            when(adminLevelService.createLevel(request))
                    .thenThrow(new AppException(AppError.LEVEL_REQUIRED_XP_INVALID));

            assertThatThrownBy(() -> controller.createLevel(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.LEVEL_REQUIRED_XP_INVALID));
        }
    }

    @Nested
    @DisplayName("updateLevel Tests")
    class UpdateLevelTests {

        @Test
        @DisplayName("Should update level successfully (PUT full update)")
        void updateLevel_Success() {
            LevelRequest request = new LevelRequest(150, "Scholar");
            AdminLevelResponse updatedResponse = new AdminLevelResponse(
                    LEVEL_NO, 150, "Scholar", LocalDateTime.now(), LocalDateTime.now()
            );
            when(adminLevelService.updateLevel(LEVEL_NO, request)).thenReturn(updatedResponse);

            ApiResponse<AdminLevelResponse> response = controller.updateLevel(LEVEL_NO, request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().requiredXp()).isEqualTo(150);
            assertThat(response.getData().title()).isEqualTo("Scholar");
            verify(adminLevelService).updateLevel(LEVEL_NO, request);
        }

        @Test
        @DisplayName("Should throw DEFAULT_LEVEL_CANNOT_BE_MODIFIED when attempting to update level 1")
        void updateLevel_LevelOne_ThrowsException() {
            LevelRequest request = new LevelRequest(0, "Default");
            when(adminLevelService.updateLevel((short) 1, request))
                    .thenThrow(new AppException(AppError.DEFAULT_LEVEL_CANNOT_BE_MODIFIED));

            assertThatThrownBy(() -> controller.updateLevel((short) 1, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.DEFAULT_LEVEL_CANNOT_BE_MODIFIED));
        }
    }

    @Nested
    @DisplayName("deleteLevel Tests")
    class DeleteLevelTests {

        @Test
        @DisplayName("Should delete level successfully when level has no users")
        void deleteLevel_Success() {
            ApiResponse<Void> response = controller.deleteLevel(LEVEL_NO);

            assertThat(response.isSuccess()).isTrue();
            verify(adminLevelService).deleteLevel(LEVEL_NO);
        }

        @Test
        @DisplayName("Should throw DEFAULT_LEVEL_CANNOT_BE_MODIFIED when attempting to delete level 1")
        void deleteLevel_LevelOne_ThrowsException() {
            doThrow(new AppException(AppError.DEFAULT_LEVEL_CANNOT_BE_MODIFIED))
                    .when(adminLevelService).deleteLevel((short) 1);

            assertThatThrownBy(() -> controller.deleteLevel((short) 1))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.DEFAULT_LEVEL_CANNOT_BE_MODIFIED));
        }

        @Test
        @DisplayName("Should throw LEVEL_HAS_ACTIVE_USERS when level has active users")
        void deleteLevel_ActiveUsers_ThrowsException() {
            doThrow(new AppException(AppError.LEVEL_HAS_ACTIVE_USERS))
                    .when(adminLevelService).deleteLevel(LEVEL_NO);

            assertThatThrownBy(() -> controller.deleteLevel(LEVEL_NO))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.LEVEL_HAS_ACTIVE_USERS));
        }
    }
}
