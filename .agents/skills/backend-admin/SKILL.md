---
name: backend-admin
description: Viết chức năng backend cho module admin của Elingo (package com.elingo.admin; controller /admin/**, AdminXxxService điều phối, @Audited, Swagger, phân trang, DTO validation, mapper). Hãy dùng skill này bất cứ khi nào người dùng nhắc đến module admin phía backend, "admin.user", "admin.premium", AdminXxxController/AdminXxxService, endpoint quản trị, thao tác của ADMIN (khóa user, duyệt, kiểm duyệt, xử lý báo cáo, quản lý gói, dashboard), audit log, kể cả khi họ không nói rõ "dùng skill admin". Không dùng cho frontend admin, và không dùng để viết test (đã có skill backend-unit-test).
---

# Viết chức năng backend cho module admin

Module `com.elingo.admin` là lớp **điều phối** cho người quản trị: nhận request, kiểm tra chính sách riêng của admin, gọi service của module khác, ghi audit và log. Nó không sở hữu dữ liệu nghiệp vụ.

Mẫu code nằm ở `references/templates.md`. Mở file đó khi bắt đầu viết controller, service, mapper, DTO.

## 0. Đọc trước khi viết

1. `.agents/rules/backend-logging.md`: chuẩn log (bắt buộc cho thao tác ghi).
2. `.agents/rules/naming-conventions.md`: đặt tên class, method, endpoint.
3. Thư mục `docs/`: tài liệu kiến trúc backend và `database-schema.md` (xem index hiện có trước khi viết query).
4. Code đã có để bắt chước đúng phong cách: `admin/common/audit` (`@Audited`, `AdminAuditAction`, `AdminAuditTargetType`), một sub-package admin gần nhất (nếu có), `AppError`, `GlobalExceptionHandler`.

Nếu trong repo đã có mẫu khác với `references/templates.md`, ưu tiên code thực tế trong repo.

## 1. Ranh giới module (quan trọng nhất)

**1.1 Admin chỉ điều phối.** Việc của admin: kiểm tra quyền, chính sách riêng của admin (ví dụ admin không được tự khóa chính mình), gọi service module khác, ghi audit, ghi log, ánh xạ DTO.

**1.2 Được và không được import từ module khác:**

| Được | Không được |
|---|---|
| Interface `XxxService` của module khác | `XxxRepository` |
| DTO công khai do module đó sở hữu (record request/response/criteria/command) và enum của module khác | `Entity`, `Specification`, `XxxMapper`, `XxxServiceImpl`, listener |

Response DTO do module nghiệp vụ sở hữu có thể được thiết kế riêng cho truy vấn quản trị (ví dụ `UserAdminResponse` trong `com.elingo.user.dto.response`). Admin được phép dùng trực tiếp DTO công khai này; không tạo thêm một response DTO trong `admin` chỉ để đổi tên hoặc sao chép cùng cấu trúc. Điều này **không** cho phép module nghiệp vụ import DTO nằm trong `com.elingo.admin.*`.

Ngoại lệ duy nhất: repository và entity của chính `admin/common/audit`.

**1.3 Không phụ thuộc chéo.** Không module nào được import `com.elingo.admin.*`. Admin phụ thuộc vào module khác, không bao giờ ngược lại.

**1.4 Hợp đồng với module gốc.** Khi cần dữ liệu hoặc thao tác mà service module gốc chưa có:
- Thêm method vào service của module gốc (đặt tên theo `naming-conventions.md`, theo kiểu các method sẵn có như `...ByAdmin`, `...ForAdmin`).
- Method nhận và trả **DTO do chính module gốc sở hữu** (criteria, command, response), không trả entity. DTO này nằm trong package `dto` của module gốc, không đặt trong `admin`. Với dữ liệu chỉ phục vụ màn hình/quy trình quản trị, có thể tạo response DTO công khai trong module gốc, ví dụ `user.dto.response.UserAdminResponse`.
- Nếu response DTO của module gốc đã đúng cấu trúc API mà endpoint admin cần, Admin dùng trực tiếp DTO đó; **không** tạo bản sao `AdminXxxResponse` và mapper tương ứng chỉ để chuyển từng field giống hệt nhau.
- Nếu API admin thực sự có cấu trúc khác, cần che bớt dữ liệu, đổi cách biểu diễn, hoặc ghép dữ liệu từ nhiều module, khi đó mới tạo response DTO/mapper riêng trong `admin`.
- Quy tắc dữ liệu (ràng buộc, unique, domain event, thông báo) nằm ở module gốc, để mọi luồng đều chạy đúng. Chính sách chỉ dành cho admin nằm ở admin service.
- Admin ánh xạ request/criteria/command của mình sang DTO của module gốc bằng mapper khi cần. Việc ánh xạ `Entity` sang response DTO thuộc mapper của module gốc; service của module gốc gọi mapper đó và trả DTO công khai cho Admin. Admin không import hoặc gọi trực tiếp mapper của module khác.

**1.5 Danh sách cần dữ liệu từ nhiều module:** gọi method **batch** theo danh sách id của module kia (ví dụ `getXByUserIds(Collection<Long>)`). Không gọi service theo từng dòng trong vòng lặp, vì đó là N+1 xuyên module.

## 2. Cấu trúc package

```
admin/<feature>/
├── controller/AdminXxxController
├── service/AdminXxxService
├── service/impl/AdminXxxServiceImpl
├── mapper/AdminXxxMapper
└── dto/request/..., dto/response/...
```

Không có `repository/`, `entity/`, `specification/` trong sub-package feature (xem 1.2). Mọi class có tiền tố `Admin` để không trùng tên với module gốc. Đường dẫn khai báo trong `AdminApiPaths` (context-path đã có `/api/v1` nên `BASE = "/admin"`). Sub-package nhỏ chỉ có 1-2 file thì không cần tạo đủ thư mục con.

## 3. Thao tác ghi: audit và log (bắt buộc)

Mọi method admin làm thay đổi dữ liệu phải có **cả hai**:

**Audit**
- Gắn `@Audited(action = ..., target = ..., targetId = "...", detail = "...")` trên method của class `*ServiceImpl` (tầng service, không đặt ở controller).
- Mỗi method chỉ một `action`, nên hai hành động khác nhau (khóa / mở khóa, bật / tắt) tách thành hai method và hai endpoint.
- `targetId` và `detail` là SpEL. Tạo mới dùng `#result.id`. Nhiều tham số dùng `#userId`, `#request.reason()` (record luôn dùng được dạng `()`). Chuỗi cố định đặt trong dấu nháy đơn.
- Hành động chưa có thì thêm hằng số vào `AdminAuditAction` / `AdminAuditTargetType` trước.
- `detail` ghi lý do hoặc giá trị mới. Tuyệt đối không ghi mật khẩu, token, dữ liệu riêng tư.
- `adminId` lấy từ `@CurrentUserId Long adminId` ở controller rồi truyền xuống service, không nhận từ body.

**Log**
- Làm theo `.agents/rules/backend-logging.md` (đó là nguồn chuẩn; nếu khác với phần này thì theo file đó). Logger dùng `@Slf4j(topic = "ADMIN-<TÊN>-SERVICE")` theo quy ước UPPER-KEBAB của dự án.
- Ghi log một lần mỗi thao tác ghi, sau khi các lời gọi module khác thành công, kèm id liên quan (adminId, id đối tượng). Không log dữ liệu nhạy cảm.

Thao tác chỉ đọc thì không cần audit và log (trừ khi quy ước logging yêu cầu khác).

## 4. Phân trang

- Controller nhận `Pageable` mặc định của Spring: `@ParameterObject @PageableDefault(size = 20, sort = "...", direction = ...) Pageable pageable`.
- **Không** đưa `page`, `size`, `sort` vào DTO, không viết `@RequestParam page/size`, không tự `PageRequest.of(...)` ở controller, không tạo class phân trang mới. Kết quả bọc bằng `PageResponse<T>` có sẵn trong `common/dto`.
- Client truyền `sort` tùy ý sẽ gây `PropertyReferenceException` (ra lỗi 500) hoặc sort theo cột không có index. Vì vậy admin service đưa `pageable` qua `AdminPageables.sanitize(pageable, ALLOWED_SORTS)` trước khi gọi module khác. `ALLOWED_SORTS` chỉ gồm các cột có index (mẫu ở `references/templates.md`). Sort sai thì trả 400, không im lặng bỏ qua.
- Trần `size` cấu hình một lần: `spring.data.web.pageable.max-page-size`. Nếu chưa có trong `application.yaml` thì đề xuất thêm.

## 5. DTO và validation

- Mọi request/response là Java `record`. Request phải kiểm tra chặt, mỗi ràng buộc có `message` là **mã lỗi** theo quy ước dự án (`INVALID_...`):
  - Chuỗi: `@NotBlank` (hoặc `@NotNull`) + `@Size(max = ...)` đúng bằng độ dài cột DB; `@Pattern`, `@Email` khi cần.
  - Số: `@NotNull` + `@Min/@Max` hoặc `@Positive/@PositiveOrZero`; id luôn `@Positive`.
  - Collection: `@NotNull` hoặc `@Size`, kiểm tra từng phần tử `List<@NotNull @Positive Long>`; record lồng nhau thêm `@Valid`.
  - Ràng buộc chéo trường (khoảng ngày `from <= to`, trường này bắt buộc khi trường kia có giá trị): `@AssertTrue` trên method `isXxx()`, kèm `@Schema(hidden = true)` để Swagger không coi nó là field.
  - Filter query: mọi trường tùy chọn nhưng vẫn có `@Size`, `@Positive`; filter theo khoảng thời gian nên chặn trần độ dài khoảng (ví dụ 366 ngày) bằng `@AssertTrue`.
- Controller luôn có `@Valid` trên `@RequestBody` và trên DTO filter. Path variable có `@Positive`.
- Mỗi record và mỗi field có `@Schema` (xem mục 8).

## 6. PUT là ghi đè

- `PUT` thay toàn bộ các trường sửa được. Giá trị `null` nghĩa là **ghi null**, không phải "bỏ qua". Không viết `if (request.x() != null) entity.setX(...)`, không dùng `NullValuePropertyMappingStrategy.IGNORE`.
- Trường bắt buộc thì bắt buộc ở DTO (`@NotNull`/`@NotBlank`), không "vá" giá trị mặc định trong service. Cột DB `NOT NULL` ⇒ field DTO tương ứng phải `@NotNull`.
- Tập hợp (tag, cấp độ...) gửi `null` được hiểu là rỗng, theo quy ước PUT hiện có của dự án.
- Không dùng `PATCH`. Chuyển trạng thái (khóa, bật/tắt...) là endpoint `PUT` riêng, có audit action riêng.
- Việc ghi đè nằm ở module gốc: admin truyền **command đầy đủ**, module gốc dùng mapper `@MappingTarget` (mặc định của MapStruct là ghi null).

## 7. Mapper

- Dùng MapStruct (`@Mapper(componentModel = "spring")`) cho các chuyển đổi thực sự cần thiết; không tạo mapper chỉ để sao chép hai response DTO có cùng cấu trúc. Không dựng object bằng `builder()` hoặc constructor dài trong service.
- Ánh xạ `Entity` → response DTO thuộc mapper của module sở hữu entity. Service module đó gọi mapper và trả DTO công khai qua interface service.
- Admin mapper chỉ dùng để chuyển request admin → criteria/command của module gốc, hoặc chuyển/ghép các DTO công khai thành response admin khi hợp đồng API thực sự khác. Mapper của admin không đụng entity của module khác.
- Admin không import mapper của module khác; muốn tái sử dụng mapping thì gọi service của module đó, để module tự dùng mapper của mình.
- Cần ghép dữ liệu từ nhiều nguồn thì khai báo mapper nhận nhiều tham số (`@Mapping(source = "a.x", target = "x")`) hoặc `default` method, không nhồi logic vào service.

## 8. Controller và Swagger (bắt buộc đầy đủ)

- Class: `@Tag(name = "Admin - <Tên>", description = "...")`, `@RestController`, `@RequestMapping(AdminApiPaths.X)`, `@PreAuthorize("hasRole('ADMIN')")`.
- Mỗi endpoint: `@Operation(summary, description)`. `description` nêu quyền, hiệu ứng phụ (audit action nào), quy tắc đáng chú ý.
- Mỗi trường hợp là **một** `@io.swagger.v3.oas.annotations.responses.ApiResponse` riêng, **không** bọc trong `@ApiResponses`. Luôn viết tên đầy đủ (fully qualified) vì trùng tên với `com.elingo.common.dto.ApiResponse`.
  - Thành công: đúng status code mà endpoint trả (200 hoặc 201), mô tả dữ liệu trả về.
  - Thất bại: mỗi status code là một `@ApiResponse` (400 validate, 401, 403, 404, 409, 500...), `description` ghi **mã lỗi chính xác** lấy từ `AppError` và message validation của DTO (ví dụ `USER_NOT_FOUND`, `CANNOT_MODIFY_SELF`, `INVALID_LOCK_REASON`). Chỉ liệt kê trường hợp thực sự xảy ra ở endpoint đó, không bịa mã lỗi; thiếu mã thì thêm vào `AppError`.
  - Lỗi dùng `content = @Content(schema = @Schema(implementation = com.elingo.common.dto.ApiResponse.class))`.
  - Request body: `@io.swagger.v3.oas.annotations.parameters.RequestBody(description, required = true, content = ...)` kèm `@RequestBody` của Spring trên tham số.
- Tham số: `@Parameter(description, example)` cho `@PathVariable`; `@ParameterObject` cho DTO filter và `Pageable` để Swagger tách thành từng query param (cần springdoc trong `pom.xml`, nếu thiếu thì nêu ra).
- DTO: `@Schema(description)` trên mỗi record, mỗi field có `description`, `example`, và `requiredMode`/`allowableValues` khi cần.
- Controller không chứa logic: nhận request, lấy `adminId`, gọi `AdminXxxService`, bọc `ApiResponse.success(...)`.

## 9. Hiệu năng

Phần lớn request admin là truy vấn nhiều dữ liệu. Các quy tắc này áp dụng cả khi bạn thêm query vào module gốc cho admin dùng.

- **Bám index có sẵn.** Mở `database-schema.md`, viết điều kiện lọc và sắp xếp theo đúng thứ tự cột của index hiện có (cột trái nhất trước).
- **Chỉ đề xuất index khi thật sự cần** (bảng lớn, truy vấn thường dùng, lọc/sắp xếp chọn lọc tốt mà không có index nào dùng được). Không đề xuất cho bảng nhỏ (gói, level, badge...). Cuối câu trả lời có mục "Index đề xuất": câu lệnh tạo index, truy vấn nào dùng nó, lý do cần, chi phí ghi thêm.
- **Tránh N+1:** danh sách trả DTO bằng projection, hoặc nạp sẵn quan hệ thực sự dùng bằng `@EntityGraph` / `join fetch`; không nạp quan hệ không dùng. Không `join fetch` collection kèm `Pageable` (Hibernate phân trang trong bộ nhớ): dùng hai bước (lấy id của trang rồi nạp theo id) hoặc `@BatchSize`.
- Dữ liệu xuyên module: method batch theo danh sách id (xem 1.5).
- `LIKE '%x%'` không dùng được index: bảng lớn thì chuyển sang tìm theo tiền tố hoặc FULLTEXT, và nói rõ khi giữ nguyên.
- Đọc dùng `@Transactional(readOnly = true)`. Thao tác hàng loạt dùng bulk query, không lặp `save`.
- Thống kê: gom nhóm ở DB, giới hạn khoảng thời gian (xem mục 5).

## 10. Quy trình

1. Xác định dữ liệu thuộc module nào, liệt kê service method cần dùng. Kiểm tra module gốc đã có chưa.
2. Thiếu thì thêm vào module gốc (mục 1.4, 9), nhớ liệt kê rõ file đã sửa ngoài `admin`.
3. Bổ sung `AdminAuditAction`, `AdminAuditTargetType`, `AppError`, `Tôi ` nếu cần.
4. Viết theo thứ tự: DTO → mapper → service + impl → controller (đủ Swagger).
5. Rà checklist bên dưới, rồi báo cáo ngắn: file thêm/sửa (tách riêng phần ngoài `admin`), index đề xuất (nếu có), điểm cần người dùng quyết định.

Không viết test trong skill này. Người dùng gọi skill `backend-unit-test` riêng sau khi feature hoàn thiện.

## Checklist trước khi kết thúc

- [ ] Không import repository, entity, specification, mapper, impl của module khác; không module nào import `admin`. Response DTO dùng xuyên module phải do module sở hữu; ưu tiên tái sử dụng DTO đó thay vì tạo bản sao trong `admin`.
- [ ] Mọi method ghi có `@Audited` (action đúng, `targetId`/`detail` SpEL đúng, không dữ liệu nhạy cảm) và log theo `backend-logging.md`.
- [ ] Không có `REQUIRES_NEW`, không nuốt lỗi lời gọi module khác.
- [ ] Controller nhận `Pageable`; không có `page/size/sort` trong DTO; sort được whitelist; trả `PageResponse<T>`.
- [ ] PUT ghi đè, không có `if (x != null)`, không `IGNORE` null; trường bắt buộc có `@NotNull`.
- [ ] Record DTO validate chặt, message là mã lỗi, `@Size` khớp độ dài cột, `@AssertTrue` cho ràng buộc chéo.
- [ ] Dùng mapper, không dựng object bằng builder/constructor dài trong service.
- [ ] Swagger đủ: `@Tag`, `@Operation`, `@Schema` DTO, mỗi `@ApiResponse` riêng (không `@ApiResponses`), mã lỗi chính xác, request body, `@ParameterObject`.
- [ ] Query bám index sẵn có; quan hệ cần dùng đã được nạp; danh sách xuyên module dùng method batch; có mục "Index đề xuất" nếu thực sự cần.