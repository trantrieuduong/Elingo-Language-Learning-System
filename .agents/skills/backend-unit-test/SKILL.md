---
name: backend-unit-test
description: Viết unit test (JUnit 5 + Mockito) cho backend Elingo (Spring Boot) theo đúng convention của dự án - nested class, phủ đủ case thành công/thất bại, DisplayName tiếng Anh, *ServiceTest đặt trong thư mục service. Dùng skill này bất cứ khi nào user nhắc tới viết unit test, thêm test, bổ sung test, tăng coverage, "viết test cho module/feature X", "test cho phần backend", hoặc muốn có test cho các thay đổi mới, kể cả khi user không nói rõ chữ "skill". Nếu user chỉ định module/feature thì viết test cho phần đó; nếu không chỉ định thì tra git để tìm feature mới và file đã thay đổi. Tự động bỏ qua các thư mục/file nằm trong sonar.coverage.exclusions của pom.xml.
---

# Elingo – Viết unit test cho backend

Chỉ viết test. Không sửa code production. Nếu một đoạn code không test được (ví dụ phụ thuộc cứng vào static/`new`), ghi lại trong báo cáo cuối thay vì tự refactor.

## Bước 1 – Xác định phạm vi (scope)

**Trường hợp A – user chỉ định module/feature** (vd: "admin.user", "vocabulary", "notification", "SystemConfigService"):
1. Tìm trong `backend/src/main/java` các package/class khớp (tên có thể viết tắt hoặc dùng dấu chấm như `admin.user` → `.../admin/user/...`). Nếu khớp nhiều chỗ, lấy tất cả các chỗ hợp lý và nói rõ trong báo cáo.
2. Phạm vi = mọi class đủ điều kiện (Bước 2) trong package đó, gồm cả package con.

**Trường hợp B – user không nói module nào:** tra git để biết cái gì mới/đổi. Chạy lần lượt:
```bash
git branch --show-current
git status --porcelain                       # chưa commit, gồm cả file untracked mới
git diff --name-only                         # unstaged
git diff --name-only --cached                # staged
git diff --name-only $(git merge-base HEAD dev)...HEAD   # các commit của nhánh hiện tại so với dev
```
Với file đã đổi, đọc `git diff` để biết method nào mới/sửa và ưu tiên test các method đó (nhưng vẫn đảm bảo test class không bỏ sót nhánh logic quan trọng của method bị ảnh hưởng). Nếu file test tương ứng đã tồn tại thì mở rộng nó, đừng tạo trùng.

Nếu cả git lẫn lời user đều không cho ra phạm vi (working tree sạch, không có commit mới), hỏi user một câu ngắn xem muốn test module nào.

## Bước 2 – Lọc theo `sonar.coverage.exclusions`

Đọc `pom.xml` (ở `backend/pom.xml`) tại thời điểm chạy, lấy giá trị `<sonar.coverage.exclusions>` làm nguồn sự thật - danh sách có thể thay đổi nên đừng tin danh sách bên dưới nếu pom khác. Bản hiện tại (để tham khảo):

```
**/config/**, **/dto/**, **/entity/**, **/exception/**, **/repository/**,
**/common/**, **/mapper/**, **/annotation/**, **/*Application.java, **/*Config.java
```

Loại bỏ mọi file khớp pattern (glob kiểu Ant: `**` = nhiều cấp thư mục). Lưu ý `**/common/**` loại cả `com.elingo.common.service.impl.*` và `**/exception/**` loại cả `GlobalExceptionHandler`. Lý do: các phần này bị loại khỏi tính coverage nên viết test là công sức không đổi lại gì. Danh sách file bị bỏ qua + pattern khớp sẽ được báo cáo ở cuối.

Những thứ còn lại thường đáng test: service impl, util/helper, event listener, scheduler, security filter/provider, validator tuỳ chỉnh, controller (xem mục Controller).

## Bước 3 – Nắm convention hiện có trước khi viết

Trước khi viết, dành ít phút xem để test mới khớp với repo:
- `pom.xml`: có `spring-boot-starter-test` không (JUnit 5, Mockito, AssertJ có sẵn), có dependency test khác không.
- Mở 1-2 file trong `src/test/java` (nếu có) để theo style đang dùng (assertion lib, tên biến, cách dựng dữ liệu).
- Class cần test: đọc toàn bộ, cộng thêm các thứ nó dùng để hiểu hành vi thật: repository method được gọi, mapper, `AppError`/`AppException`, DTO record, policy như `DeckAccessPolicy`. Không đoán chữ ký method - test mà không compile là test vô giá trị.

## Bước 4 – Quy ước đặt tên và vị trí file

Test đặt dưới `src/test/java`, **phản chiếu package của source**, với 2 ngoại lệ cho service:

| Source | Test |
|---|---|
| `.../admin/user/service/impl/UserServiceImpl.java` | `.../admin/user/service/UserServiceTest.java` |
| `.../x/service/ReportService.java` (class thường, không có impl) | `.../x/service/ReportServiceTest.java` |
| `.../x/util/SlugUtils.java` | `.../x/util/SlugUtilsTest.java` |
| `.../x/listener/XpListener.java` | `.../x/listener/XpListenerTest.java` |

Quy tắc: bỏ hậu tố `Impl` khỏi tên class và bỏ segment `impl` khỏi package, rồi thêm `Test`. Tức là **`*ServiceTest` nằm trong `service/`, không phải `*ServiceImplTest` trong `service/impl/`**. Test viết cho implementation, không viết cho interface.

## Bước 5 – Cấu trúc test class

Dùng `@ExtendWith(MockitoExtension.class)`, `@Mock` cho dependency, `@InjectMocks` cho class cần test, tag `@Tag("unit")`. Gom test theo **`@Nested` class, mỗi public method một nested class**. Mọi `@DisplayName` viết bằng **tiếng Anh**.

```java
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("UserService")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @InjectMocks private UserServiceImpl userService;

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("should return user when id exists")
        void shouldReturnUser_whenIdExists() {
            // given
            ...
            // when
            ...
            // then
            ...
        }

        @Test
        @DisplayName("should throw USER_NOT_FOUND when id does not exist")
        void shouldThrowNotFound_whenIdDoesNotExist() { ... }
    }
}
```

- Tên method test: `shouldXxx_whenYyy`. Nested class đặt theo tên method nguồn (PascalCase).
- Mỗi test chỉ kiểm một hành vi. Bố cục given/when/then bằng comment cho dễ đọc.
- Dùng `ArgumentCaptor` khi cần kiểm tra entity được lưu có đúng giá trị. Ở nhánh lỗi, `verify(repo, never()).save(any())` để chứng minh không có tác dụng phụ.
- Dùng `@ParameterizedTest` khi nhiều input cho cùng một hành vi (giá trị biên, enum), tránh copy-paste.
- Không test private method, không test getter/setter thuần, không mock thứ có thể dựng thật một cách rẻ (record, DTO, entity builder).
- Tránh `Mockito.lenient()` và `any()` bừa bãi - strict stubs giúp phát hiện stub thừa. Nếu bị `UnnecessaryStubbingException` thì sửa test, đừng nới lỏng.

## Bước 6 – "Đủ case" nghĩa là gì

Với mỗi public method, liệt kê trước rồi mới viết. Cân nhắc đủ các nhóm sau (nhóm nào không áp dụng thì bỏ):

**Thành công (happy path)**: luồng chính, kết quả trả về đúng, tương tác với dependency đúng (save/publish event/gọi mapper...).

**Thất bại / ngoại lệ**:
- Không tìm thấy (repository trả `Optional.empty()`, list rỗng).
- Vi phạm quy tắc nghiệp vụ: trùng dữ liệu, vượt giới hạn, trạng thái không hợp lệ, sai quyền sở hữu/không có quyền (vd. policy ném `AppException`).
- Dependency ném exception - phần xử lý có đúng không.

**Biên và dữ liệu đặc biệt**: null, chuỗi rỗng/blank, 0, giá trị âm, đúng bằng ngưỡng giới hạn, trang đầu/trang cuối/trang rỗng với phân trang, danh sách rỗng, một phần tử.

**Nhánh rẽ**: mọi `if/else`, `switch`, toán tử ba ngôi, `Optional.orElse...`, vòng lặp có điều kiện thoát sớm - mỗi nhánh có ít nhất một test chạm tới.

Khi assert ngoại lệ của dự án: ném `AppException` kèm `AppError` - assert đúng error code bằng cách xem class `AppException` để biết getter, không chỉ `isInstanceOf`. Giữ nguyên message/code theo source thật.

**Thời gian**: code dự án dùng `LocalDateTime.now()` trực tiếp, nên đừng assert bằng đúng một thời điểm; dùng `isNotNull()`, `isCloseTo(now, within(...))` hoặc kiểm tra quan hệ trước/sau. Không tự ý đổi code sang `Clock`.

## Controller

Controller không nằm trong exclusions nên đủ điều kiện. Chỉ viết test controller khi user nhắc tới hoặc controller nằm trong phạm vi thay đổi, và chỉ khi tìm được cách chạy nhanh, không cần dựng toàn bộ context: ưu tiên `MockMvcBuilders.standaloneSetup(controller)` với Mockito (kèm `HandlerMethodArgumentResolver` giả cho `@CurrentUserId` nếu cần). Kiểm tra status, JSON `ApiResponse` (`success`, `data`), và lỗi validation. Nếu repo đã có kiểu test controller khác thì theo kiểu đó. Nếu phải tự bịa nhiều hạ tầng mới (vd. tính năng của Spring Boot 4 mà chưa chắc về API), hãy nói rõ và dừng ở phần service thay vì gửi test không chắc chạy.

## Bước 7 – Chạy và sửa

Sau khi viết, chạy thật:
```bash
cd backend
./mvnw -q test -Dtest='UserServiceTest,...'     # hoặc mvn nếu không có wrapper
```
Sửa cho đến khi compile được và xanh. Nếu một test đỏ vì **code production có bug thật**, đừng bẻ assertion cho khớp - giữ test đúng kỳ vọng hợp lý, đánh dấu rõ trong báo cáo và hỏi user. Không sửa/xoá test cũ đang xanh trừ khi thay đổi hợp lệ của code làm chúng hỏng, và khi đó nói rõ.

Nếu không chạy được (thiếu môi trường, mạng), nói thẳng là chưa chạy, không khẳng định là đã pass.

## Bước 8 – Báo cáo cuối (ngắn gọn)

1. Phạm vi đã dùng: "theo yêu cầu: admin.user" hoặc "theo git: các file đổi so với dev".
2. Danh sách file test đã tạo/cập nhật + số test mỗi file.
3. File bị bỏ qua do `sonar.coverage.exclusions` (kèm pattern khớp) và file bỏ qua vì lý do khác (nếu có).
4. Kết quả chạy test.
5. Điểm đáng lưu ý: code khó test, nghi ngờ bug, nhánh chưa phủ được và lý do.