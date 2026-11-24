# Mẫu code cho module admin

Tên class và field chỉ để minh họa, thay bằng tên thật. Mẫu dùng `ApiResponse<T>` và `PageResponse<T>` có sẵn trong `com.elingo.common.dto`, kiểm tra tên factory method thực tế (`success(...)`).

Mục lục: 1. Request DTO · 2. Filter DTO · 3. Response DTO · 4. Mapper · 5. Service · 6. Controller · 7. AdminPageables · 8. Cấu hình

## 1. Request DTO (ghi)

```java
@Schema(description = "Request payload for locking a user account")
public record LockUserRequest(
        @Schema(
                description = "Administrative reason for locking or deactivating the user account",
                example = "Violation of terms: sending unsolicited spam messages",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 500
        )
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 500, message = "INVALID_REQUEST")
        String reason
) {
}
```

PUT cập nhật: mọi field sửa được đều có mặt, trường bắt buộc là `@NotNull`/`@NotBlank`, trường tùy chọn cho phép `null` và `null` sẽ được ghi.

## 2. Filter DTO (query)

```java
@Schema(description = "Request query parameters for searching and filtering users in the admin console")
public record AdminUserSearchRequest(
        @Schema(
                description = "Keyword to search across username, email, or full name",
                example = "john",
                maxLength = 100
        )
        @Size(max = 100, message = "INVALID_KEYWORD")
        String keyword,

        @Schema(
                description = "Filter by user account role",
                example = "USER"
        )
        Role role,

        @Schema(
                description = "Filter by email verification status",
                example = "true"
        )
        Boolean isVerified,

        @Schema(
                description = "Filter by account active/lock status (true = active, false = locked)",
                example = "true"
        )
        Boolean isActive,

        @Schema(
                description = "Filter users registered on or after this date (ISO format: YYYY-MM-DD)",
                example = "2026-01-01"
        )
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate createdFrom,

        @Schema(
                description = "Filter users registered on or before this date (ISO format: YYYY-MM-DD)",
                example = "2026-12-31"
        )
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate createdTo
) {
    @AssertTrue(message = "INVALID_DATE_RANGE")
    public boolean isValidDateRange() {
        return createdFrom == null || createdTo == null || !createdFrom.isAfter(createdTo);
    }
}
```

Không có `page`, `size`, `sort` trong record này.

## 3. Response DTO

```java
@Schema(description = "Response payload containing comprehensive user profile information for administration")
public record AdminUserResponse(
        @Schema(description = "Unique identifier of the user", example = "10")
        Long id,

        @Schema(description = "Unique username of the user", example = "john_doe")
        String username,

        @Schema(description = "Email address of the user", example = "john.doe@example.com")
        String email,

        @Schema(description = "Full name of the user", example = "John Doe")
        String fullName,

        @Schema(
                description = "Public CDN URL of the user avatar image",
                example = "https://cdn.elingo.com/avatars/user-10.png"
        )
        @BuildMediaUrl String avatarUrl,

        @Schema(description = "System role of the user (USER or ADMIN)", example = "USER")
        Role role,

        @Schema(description = "Whether the email address has been verified", example = "true")
        Boolean isVerified,

        @Schema(description = "Whether the user account is active (false if locked)", example = "true")
        Boolean isActive,

        @Schema(
                description = "Timestamp when the password was last changed",
                example = "2026-02-15T08:30:00"
        )
        LocalDateTime passwordChangedAt,

        @Schema(
                description = "Timestamp when the username was last updated",
                example = "2026-01-10T14:20:00"
        )
        LocalDateTime usernameChangedAt,

        @Schema(description = "Whether the user has set a local password", example = "true")
        Boolean hasPassword,

        @Schema(description = "Whether the account is linked to Google OAuth", example = "false")
        Boolean linkedGoogle,

        @Schema(
                description = "Timestamp when the user account was registered",
                example = "2026-01-01T10:00:00"
        )
        LocalDateTime createdAt,

        @Schema(
                description = "Timestamp when the user account was last updated",
                example = "2026-02-15T08:30:00"
        )
        LocalDateTime updatedAt
) {
}
```

## 4. Mapper

Entity → DTO admin do mapper của module gốc làm. Không cần mapper riêng bên admin:

```java
// Trong module user: UserMapper
@Mapping(target = "hasPassword", expression = "java(user.getPassword() != null)")
@Mapping(target = "linkedGoogle", expression = "java(user.getGoogleProviderId() != null)")
AdminUserResponse toAdminUserResponse(User user);
```

## 5. Service

Module gốc (ví dụ `UserService`) cung cấp, đặt dưới comment nhóm:

```java
// Admin user management
PageResponse<AdminUserResponse> searchUsersForAdmin(AdminUserSearchRequest request, Pageable pageable);
AdminUserResponse getById(Long userId);
AdminUserResponse lockUser(Long adminId, Long userId, String reason);
AdminUserResponse unlockUser(Long adminId, Long userId);
```

Admin service chỉ điều phối, audit, log:

```java
public interface AdminUserService {
    PageResponse<AdminUserResponse> search(AdminUserSearchRequest request, Pageable pageable);
    AdminUserResponse lockUser(Long adminId, Long userId, LockUserRequest request);
}
```

```java
@Slf4j(topic = "ADMIN-USER-SERVICE")   // tên topic và nội dung log theo .agents/rules/backend-logging.md
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private static final Set<String> ALLOWED_SORTS = Set.of("createdAt", "id");   // chỉ cột có index

    private final UserService userService;      // service của module user, KHÔNG phải UserRepository

    @Override
    public PageResponse<AdminUserResponse> search(AdminUserSearchRequest request, Pageable pageable) {
        return userService.searchUsersForAdmin(request, AdminPageables.sanitize(pageable, ALLOWED_SORTS));
    }

    @Override
    @Audited(action = USER_LOCK, target = USER, targetId = "#userId", detail = "#request.reason()")
    public AdminUserResponse lockUser(Long adminId, Long userId, LockUserRequest request) {
        // Quy tắc dữ liệu (không tự khóa mình, trạng thái hợp lệ...) nằm ở userService.lockUser.
        AdminUserResponse locked = userService.lockUser(adminId, userId, request.reason());
        log.info("User locked userId={} byAdminId={}", userId, adminId);
        return locked;
    }
}
```

Danh sách cần dữ liệu từ module khác: gọi một lần `premiumService.getStatusByUserIds(ids)` rồi ghép bằng mapper admin (mục 4), không gọi trong vòng lặp.

## 6. Controller với Swagger đầy đủ

```java
    @PutMapping("/{userId}/unlock")
    @Operation(
            summary = "Unlock user account",
            description = "Reactivates a previously locked user account and clears ban reason. Administrators cannot unlock themselves or other administrators."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User unlocked successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "UnlockUserSuccessResponse",
                            summary = "Sample unlocked user response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 10,
                                        "username": "john_doe",
                                        "email": "john.doe@example.com",
                                        "fullName": "John Doe",
                                        "avatarUrl": "https://cdn.elingo.com/avatars/user-10.png",
                                        "role": "USER",
                                        "isVerified": true,
                                        "isActive": true,
                                        "passwordChangedAt": "2026-02-15T08:30:00",
                                        "usernameChangedAt": "2026-01-10T14:20:00",
                                        "hasPassword": true,
                                        "linkedGoogle": false,
                                        "createdAt": "2026-01-01T10:00:00",
                                        "updatedAt": "2026-02-15T08:30:00"
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - Cannot unlock self or other admins, or user already active",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "CannotModifySelfResponse",
                                    summary = "Self modification rejection",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "CANNOT_MODIFY_SELF",
                                                "message": "You cannot perform this action on your own account"
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "CannotModifyAdminResponse",
                                    summary = "Target is administrator rejection",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "CANNOT_MODIFY_ADMIN",
                                                "message": "Cannot perform administrative actions on another administrator"
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "UserAlreadyActiveResponse",
                                    summary = "User already active",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "USER_ALREADY_ACTIVE",
                                                "message": "User account is already active"
                                              }
                                            }
                                            """
                            )
                    }
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication token is missing or expired"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Not Found - User does not exist",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "UserNotFoundResponse",
                            summary = "User not found error",
                            value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "USER_NOT_FOUND",
                                        "message": "User not found"
                                      }
                                    }
                                    """
                    )
            )
    )

    public ApiResponse<AdminUserResponse> unlockUser(
            @CurrentUserId Long adminId,
            @Parameter(description = "Identifier of the user to unlock", example = "10", required = true)
            @PathVariable Long userId) {
        return ApiResponse.<AdminUserResponse>builder()
                .success(true)
                .data(adminUserService.unlockUser(adminId, userId))
                .build();
    }
```

Imports gốc: `io.swagger.v3.oas.annotations.{Operation, Parameter}`, `io.swagger.v3.oas.annotations.media.{Content, Schema}`, `io.swagger.v3.oas.annotations.tags.Tag`, `org.springdoc.core.annotations.ParameterObject`. Hai annotation trùng tên (`ApiResponse`, `RequestBody`) của Swagger luôn viết tên đầy đủ.

Chỉ liệt kê mã lỗi thực sự xảy ra. Kiểm tra `AppError` và cách `GlobalExceptionHandler` / `SecurityConfig` trả 401, 403 để ghi đúng mã.

## 7. AdminPageables (đặt trong admin/common nếu chưa có)

```java
public final class AdminPageables {

    private AdminPageables() {}

    /** Chỉ cho sort theo các thuộc tính trong allowed; ngoài danh sách thì trả 400. */
    public static Pageable sanitize(Pageable pageable, Set<String> allowed) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new AppException(AppError.INVALID_SORT);   // thêm vào AppError (400) nếu chưa có
            }
        }
        return pageable;
    }
}
```

Property sort đi thẳng vào truy vấn của module gốc nên phải là tên field của entity module gốc. Khi hai tên khác nhau, ánh xạ trước khi truyền xuống.

## 8. Cấu hình

```yaml
spring:
  data:
    web:
      pageable:
        max-page-size: 100
```
