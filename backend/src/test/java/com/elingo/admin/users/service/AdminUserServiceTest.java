package com.elingo.admin.users.service;

import com.elingo.user.dto.request.AdminUserSearchRequest;
import com.elingo.admin.users.dto.request.LockUserRequest;
import com.elingo.user.dto.response.AdminUserResponse;
import com.elingo.admin.users.service.impl.AdminUserServiceImpl;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.user.entity.Role;
import com.elingo.user.service.UserService;
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
@DisplayName("AdminUserService Tests")
class AdminUserServiceTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

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
    @DisplayName("search Tests")
    class SearchTests {

        @Test
        @DisplayName("Should delegate to userService.searchUsersForAdmin and return page response")
        void search_WhenValidRequest_ShouldReturnPageResponse() {
            AdminUserSearchRequest request = new AdminUserSearchRequest(
                    "john",
                    Role.USER,
                    true,
                    true,
                    LocalDate.now().minusDays(7),
                    LocalDate.now()
            );
            Pageable pageable = PageRequest.of(0, 10);
            PageResponse<AdminUserResponse> expectedPage = new PageResponse<>(
                    List.of(SAMPLE_USER_RESPONSE),
                    1,
                    10,
                    1L,
                    1,
                    true
            );

            when(userService.searchUsersForAdmin(request, pageable)).thenReturn(expectedPage);

            PageResponse<AdminUserResponse> result = adminUserService.search(request, pageable);

            assertThat(result).isNotNull();
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().getFirst().id()).isEqualTo(USER_ID);
            assertThat(result.totalElements()).isEqualTo(1L);
            verify(userService).searchUsersForAdmin(request, pageable);
        }

        @Test
        @DisplayName("Should pass sanitized pageable when sort is allowed")
        void search_WhenSortAllowed_ShouldPassSanitizedPageable() {
            AdminUserSearchRequest request = new AdminUserSearchRequest(null, null, null, null, null, null);
            Pageable sortedPageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("createdAt"));
            PageResponse<AdminUserResponse> expectedPage = new PageResponse<>(List.of(), 1, 10, 0L, 0, true);

            when(userService.searchUsersForAdmin(request, sortedPageable)).thenReturn(expectedPage);

            PageResponse<AdminUserResponse> result = adminUserService.search(request, sortedPageable);

            assertThat(result).isNotNull();
            verify(userService).searchUsersForAdmin(request, sortedPageable);
        }

        @Test
        @DisplayName("Should throw INVALID_SORT when sort field is not allowed")
        void search_WhenSortNotAllowed_ShouldThrowInvalidSort() {
            AdminUserSearchRequest request = new AdminUserSearchRequest(null, null, null, null, null, null);
            Pageable invalidSortPageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("passwordHash"));

            assertThatThrownBy(() -> adminUserService.search(request, invalidSortPageable))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_SORT));
        }

        @Test
        @DisplayName("Should propagate exception when userService throws exception")
        void search_WhenUserServiceThrowsException_ShouldPropagate() {
            AdminUserSearchRequest request = new AdminUserSearchRequest(null, null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 10);

            when(userService.searchUsersForAdmin(request, pageable))
                    .thenThrow(new AppException(AppError.INVALID_REQUEST));

            assertThatThrownBy(() -> adminUserService.search(request, pageable))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_REQUEST));
            verify(userService).searchUsersForAdmin(request, pageable);
        }
    }

    @Nested
    @DisplayName("getById Tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should return user response when user exists")
        void getById_WhenUserExists_ShouldReturnResponse() {
            when(userService.getById(USER_ID)).thenReturn(SAMPLE_USER_RESPONSE);

            AdminUserResponse result = adminUserService.getById(USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(USER_ID);
            assertThat(result.username()).isEqualTo("john_doe");
            verify(userService).getById(USER_ID);
        }

        @Test
        @DisplayName("Should propagate USER_NOT_FOUND exception when user does not exist")
        void getById_WhenUserNotFound_ShouldPropagateException() {
            when(userService.getById(999L))
                    .thenThrow(new AppException(AppError.USER_NOT_FOUND));

            assertThatThrownBy(() -> adminUserService.getById(999L))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));
            verify(userService).getById(999L);
        }
    }

    @Nested
    @DisplayName("lockUser Tests")
    class LockUserTests {

        @Test
        @DisplayName("Should delegate to userService.lockUser and return locked user response")
        void lockUser_WhenValid_ShouldReturnLockedUserResponse() {
            LockUserRequest request = new LockUserRequest("Violation of community guidelines");
            when(userService.lockUser(ADMIN_ID, USER_ID, request.reason()))
                    .thenReturn(LOCKED_USER_RESPONSE);

            AdminUserResponse result = adminUserService.lockUser(ADMIN_ID, USER_ID, request);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(USER_ID);
            assertThat(result.isActive()).isFalse();
            verify(userService).lockUser(ADMIN_ID, USER_ID, request.reason());
        }

        @Test
        @DisplayName("Should propagate CANNOT_MODIFY_SELF exception when admin tries to lock self")
        void lockUser_WhenAdminLocksSelf_ShouldPropagateException() {
            LockUserRequest request = new LockUserRequest("Test reason");
            when(userService.lockUser(ADMIN_ID, ADMIN_ID, request.reason()))
                    .thenThrow(new AppException(AppError.CANNOT_MODIFY_SELF));

            assertThatThrownBy(() -> adminUserService.lockUser(ADMIN_ID, ADMIN_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CANNOT_MODIFY_SELF));
            verify(userService).lockUser(ADMIN_ID, ADMIN_ID, request.reason());
        }

        @Test
        @DisplayName("Should propagate CANNOT_MODIFY_ADMIN exception when target user is an administrator")
        void lockUser_WhenTargetIsAdmin_ShouldPropagateException() {
            LockUserRequest request = new LockUserRequest("Test reason");
            when(userService.lockUser(ADMIN_ID, USER_ID, request.reason()))
                    .thenThrow(new AppException(AppError.CANNOT_MODIFY_ADMIN));

            assertThatThrownBy(() -> adminUserService.lockUser(ADMIN_ID, USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CANNOT_MODIFY_ADMIN));
            verify(userService).lockUser(ADMIN_ID, USER_ID, request.reason());
        }

        @Test
        @DisplayName("Should propagate USER_INACTIVE exception when user is already locked")
        void lockUser_WhenUserAlreadyInactive_ShouldPropagateException() {
            LockUserRequest request = new LockUserRequest("Test reason");
            when(userService.lockUser(ADMIN_ID, USER_ID, request.reason()))
                    .thenThrow(new AppException(AppError.USER_INACTIVE));

            assertThatThrownBy(() -> adminUserService.lockUser(ADMIN_ID, USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_INACTIVE));
            verify(userService).lockUser(ADMIN_ID, USER_ID, request.reason());
        }

        @Test
        @DisplayName("Should propagate USER_NOT_FOUND exception when user does not exist")
        void lockUser_WhenUserNotFound_ShouldPropagateException() {
            LockUserRequest request = new LockUserRequest("Test reason");
            when(userService.lockUser(ADMIN_ID, 999L, request.reason()))
                    .thenThrow(new AppException(AppError.USER_NOT_FOUND));

            assertThatThrownBy(() -> adminUserService.lockUser(ADMIN_ID, 999L, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));
            verify(userService).lockUser(ADMIN_ID, 999L, request.reason());
        }
    }

    @Nested
    @DisplayName("unlockUser Tests")
    class UnlockUserTests {

        @Test
        @DisplayName("Should delegate to userService.unlockUser and return unlocked user response")
        void unlockUser_WhenValid_ShouldReturnUnlockedUserResponse() {
            when(userService.unlockUser(ADMIN_ID, USER_ID)).thenReturn(SAMPLE_USER_RESPONSE);

            AdminUserResponse result = adminUserService.unlockUser(ADMIN_ID, USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(USER_ID);
            assertThat(result.isActive()).isTrue();
            verify(userService).unlockUser(ADMIN_ID, USER_ID);
        }

        @Test
        @DisplayName("Should propagate CANNOT_MODIFY_SELF exception when admin tries to unlock self")
        void unlockUser_WhenAdminUnlocksSelf_ShouldPropagateException() {
            when(userService.unlockUser(ADMIN_ID, ADMIN_ID))
                    .thenThrow(new AppException(AppError.CANNOT_MODIFY_SELF));

            assertThatThrownBy(() -> adminUserService.unlockUser(ADMIN_ID, ADMIN_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CANNOT_MODIFY_SELF));
            verify(userService).unlockUser(ADMIN_ID, ADMIN_ID);
        }

        @Test
        @DisplayName("Should propagate CANNOT_MODIFY_ADMIN exception when target user is an administrator")
        void unlockUser_WhenTargetIsAdmin_ShouldPropagateException() {
            when(userService.unlockUser(ADMIN_ID, USER_ID))
                    .thenThrow(new AppException(AppError.CANNOT_MODIFY_ADMIN));

            assertThatThrownBy(() -> adminUserService.unlockUser(ADMIN_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CANNOT_MODIFY_ADMIN));
            verify(userService).unlockUser(ADMIN_ID, USER_ID);
        }

        @Test
        @DisplayName("Should propagate USER_ALREADY_ACTIVE exception when user is not locked")
        void unlockUser_WhenUserAlreadyActive_ShouldPropagateException() {
            when(userService.unlockUser(ADMIN_ID, USER_ID))
                    .thenThrow(new AppException(AppError.USER_ALREADY_ACTIVE));

            assertThatThrownBy(() -> adminUserService.unlockUser(ADMIN_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_ALREADY_ACTIVE));
            verify(userService).unlockUser(ADMIN_ID, USER_ID);
        }

        @Test
        @DisplayName("Should propagate USER_NOT_FOUND exception when user does not exist")
        void unlockUser_WhenUserNotFound_ShouldPropagateException() {
            when(userService.unlockUser(ADMIN_ID, 999L))
                    .thenThrow(new AppException(AppError.USER_NOT_FOUND));

            assertThatThrownBy(() -> adminUserService.unlockUser(ADMIN_ID, 999L))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));
            verify(userService).unlockUser(ADMIN_ID, 999L);
        }
    }
}
