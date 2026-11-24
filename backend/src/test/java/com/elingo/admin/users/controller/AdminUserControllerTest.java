package com.elingo.admin.users.controller;

import com.elingo.user.dto.request.AdminUserSearchRequest;
import com.elingo.admin.users.dto.request.LockUserRequest;
import com.elingo.user.dto.response.AdminUserResponse;
import com.elingo.admin.users.service.AdminUserService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.user.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("AdminUserController Tests")
class AdminUserControllerTest {

    @Mock
    private AdminUserService adminUserService;

    @InjectMocks
    private AdminUserController adminUserController;

    private static final Long ADMIN_ID = 1L;
    private static final Long USER_ID = 2L;

    private static final AdminUserResponse SAMPLE_USER_RESPONSE = new AdminUserResponse(
            USER_ID,
            "john_doe",
            "john@example.com",
            "John Doe",
            "avatar.png",
            Role.USER,
            true,
            true,
            LocalDateTime.now(),
            LocalDateTime.now(),
            true,
            false,
            LocalDateTime.now(),
            LocalDateTime.now()
    );

    private static final AdminUserResponse LOCKED_USER_RESPONSE = new AdminUserResponse(
            USER_ID,
            "john_doe",
            "john@example.com",
            "John Doe",
            "avatar.png",
            Role.USER,
            true,
            false,
            LocalDateTime.now(),
            LocalDateTime.now(),
            true,
            false,
            LocalDateTime.now(),
            LocalDateTime.now()
    );

    @Nested
    @DisplayName("searchUsers Tests")
    class SearchUsersTests {

        @Test
        @DisplayName("Should return ApiResponse with PageResponse data when valid search request")
        void searchUsers_WhenValidRequest_ShouldReturnPageResponse() {
            AdminUserSearchRequest request = new AdminUserSearchRequest(
                    "john",
                    Role.USER,
                    true,
                    true,
                    LocalDate.now().minusDays(5),
                    LocalDate.now()
            );
            Pageable pageable = PageRequest.of(0, 20);
            PageResponse<AdminUserResponse> pageResponse = new PageResponse<>(
                    List.of(SAMPLE_USER_RESPONSE),
                    1,
                    20,
                    1L,
                    1,
                    true
            );

            when(adminUserService.search(request, pageable)).thenReturn(pageResponse);

            ApiResponse<PageResponse<AdminUserResponse>> response = adminUserController.searchUsers(request, pageable);

            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData().content()).hasSize(1);
            assertThat(response.getData().content().getFirst().username()).isEqualTo("john_doe");
            verify(adminUserService).search(request, pageable);
        }

        @Test
        @DisplayName("Should propagate exception when adminUserService throws exception")
        void searchUsers_WhenServiceThrows_ShouldPropagateException() {
            AdminUserSearchRequest request = new AdminUserSearchRequest(null, null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 20);

            when(adminUserService.search(request, pageable))
                    .thenThrow(new AppException(AppError.INVALID_REQUEST));

            assertThatThrownBy(() -> adminUserController.searchUsers(request, pageable))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_REQUEST));
            verify(adminUserService).search(request, pageable);
        }
    }

    @Nested
    @DisplayName("getUserById Tests")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should return ApiResponse with user response when user exists")
        void getUserById_WhenUserExists_ShouldReturnUserResponse() {
            when(adminUserService.getById(USER_ID)).thenReturn(SAMPLE_USER_RESPONSE);

            ApiResponse<AdminUserResponse> response = adminUserController.getUserById(USER_ID);

            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData().id()).isEqualTo(USER_ID);
            verify(adminUserService).getById(USER_ID);
        }

        @Test
        @DisplayName("Should propagate USER_NOT_FOUND when user does not exist")
        void getUserById_WhenUserNotFound_ShouldPropagateException() {
            when(adminUserService.getById(999L))
                    .thenThrow(new AppException(AppError.USER_NOT_FOUND));

            assertThatThrownBy(() -> adminUserController.getUserById(999L))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));
            verify(adminUserService).getById(999L);
        }
    }

    @Nested
    @DisplayName("lockUser Tests")
    class LockUserTests {

        @Test
        @DisplayName("Should return ApiResponse with locked user when lock request is valid")
        void lockUser_WhenValid_ShouldReturnLockedUser() {
            LockUserRequest request = new LockUserRequest("Violation of terms");
            when(adminUserService.lockUser(ADMIN_ID, USER_ID, request)).thenReturn(LOCKED_USER_RESPONSE);

            ApiResponse<AdminUserResponse> response = adminUserController.lockUser(ADMIN_ID, USER_ID, request);

            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData().id()).isEqualTo(USER_ID);
            assertThat(response.getData().isActive()).isFalse();
            verify(adminUserService).lockUser(ADMIN_ID, USER_ID, request);
        }

        @Test
        @DisplayName("Should propagate CANNOT_MODIFY_SELF when admin tries to lock self")
        void lockUser_WhenAdminLocksSelf_ShouldPropagateException() {
            LockUserRequest request = new LockUserRequest("Test reason");
            when(adminUserService.lockUser(ADMIN_ID, ADMIN_ID, request))
                    .thenThrow(new AppException(AppError.CANNOT_MODIFY_SELF));

            assertThatThrownBy(() -> adminUserController.lockUser(ADMIN_ID, ADMIN_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CANNOT_MODIFY_SELF));
            verify(adminUserService).lockUser(ADMIN_ID, ADMIN_ID, request);
        }

        @Test
        @DisplayName("Should propagate CANNOT_MODIFY_ADMIN when admin tries to lock another admin")
        void lockUser_WhenTargetIsAdmin_ShouldPropagateException() {
            LockUserRequest request = new LockUserRequest("Test reason");
            when(adminUserService.lockUser(ADMIN_ID, USER_ID, request))
                    .thenThrow(new AppException(AppError.CANNOT_MODIFY_ADMIN));

            assertThatThrownBy(() -> adminUserController.lockUser(ADMIN_ID, USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CANNOT_MODIFY_ADMIN));
            verify(adminUserService).lockUser(ADMIN_ID, USER_ID, request);
        }

        @Test
        @DisplayName("Should propagate USER_INACTIVE when user is already locked")
        void lockUser_WhenUserAlreadyInactive_ShouldPropagateException() {
            LockUserRequest request = new LockUserRequest("Test reason");
            when(adminUserService.lockUser(ADMIN_ID, USER_ID, request))
                    .thenThrow(new AppException(AppError.USER_INACTIVE));

            assertThatThrownBy(() -> adminUserController.lockUser(ADMIN_ID, USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_INACTIVE));
            verify(adminUserService).lockUser(ADMIN_ID, USER_ID, request);
        }

        @Test
        @DisplayName("Should propagate USER_NOT_FOUND when user does not exist")
        void lockUser_WhenUserNotFound_ShouldPropagateException() {
            LockUserRequest request = new LockUserRequest("Test reason");
            when(adminUserService.lockUser(ADMIN_ID, 999L, request))
                    .thenThrow(new AppException(AppError.USER_NOT_FOUND));

            assertThatThrownBy(() -> adminUserController.lockUser(ADMIN_ID, 999L, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));
            verify(adminUserService).lockUser(ADMIN_ID, 999L, request);
        }
    }

    @Nested
    @DisplayName("unlockUser Tests")
    class UnlockUserTests {

        @Test
        @DisplayName("Should return ApiResponse with unlocked user when unlock request is valid")
        void unlockUser_WhenValid_ShouldReturnUnlockedUser() {
            when(adminUserService.unlockUser(ADMIN_ID, USER_ID)).thenReturn(SAMPLE_USER_RESPONSE);

            ApiResponse<AdminUserResponse> response = adminUserController.unlockUser(ADMIN_ID, USER_ID);

            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData().id()).isEqualTo(USER_ID);
            assertThat(response.getData().isActive()).isTrue();
            verify(adminUserService).unlockUser(ADMIN_ID, USER_ID);
        }

        @Test
        @DisplayName("Should propagate CANNOT_MODIFY_SELF when admin tries to unlock self")
        void unlockUser_WhenAdminUnlocksSelf_ShouldPropagateException() {
            when(adminUserService.unlockUser(ADMIN_ID, ADMIN_ID))
                    .thenThrow(new AppException(AppError.CANNOT_MODIFY_SELF));

            assertThatThrownBy(() -> adminUserController.unlockUser(ADMIN_ID, ADMIN_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CANNOT_MODIFY_SELF));
            verify(adminUserService).unlockUser(ADMIN_ID, ADMIN_ID);
        }

        @Test
        @DisplayName("Should propagate CANNOT_MODIFY_ADMIN when admin tries to unlock another admin")
        void unlockUser_WhenTargetIsAdmin_ShouldPropagateException() {
            when(adminUserService.unlockUser(ADMIN_ID, USER_ID))
                    .thenThrow(new AppException(AppError.CANNOT_MODIFY_ADMIN));

            assertThatThrownBy(() -> adminUserController.unlockUser(ADMIN_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CANNOT_MODIFY_ADMIN));
            verify(adminUserService).unlockUser(ADMIN_ID, USER_ID);
        }

        @Test
        @DisplayName("Should propagate USER_ALREADY_ACTIVE when user is not locked")
        void unlockUser_WhenUserAlreadyActive_ShouldPropagateException() {
            when(adminUserService.unlockUser(ADMIN_ID, USER_ID))
                    .thenThrow(new AppException(AppError.USER_ALREADY_ACTIVE));

            assertThatThrownBy(() -> adminUserController.unlockUser(ADMIN_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_ALREADY_ACTIVE));
            verify(adminUserService).unlockUser(ADMIN_ID, USER_ID);
        }

        @Test
        @DisplayName("Should propagate USER_NOT_FOUND when user does not exist")
        void unlockUser_WhenUserNotFound_ShouldPropagateException() {
            when(adminUserService.unlockUser(ADMIN_ID, 999L))
                    .thenThrow(new AppException(AppError.USER_NOT_FOUND));

            assertThatThrownBy(() -> adminUserController.unlockUser(ADMIN_ID, 999L))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));
            verify(adminUserService).unlockUser(ADMIN_ID, 999L);
        }
    }
}
