# Quy tắc đặt tên - Elingo

Áp dụng cho `backend` (Java Spring Boot), `frontend` (React + JS).

## 1. Quy tắc chung

- Tên (thư mục, file, class, hàm, biến, constant) **dùng tiếng Anh**, không dùng tiếng Việt.
- Tên mô tả rõ mục đích; không dùng viết tắt mơ hồ (`tmp`, `data`, `obj`, `mgr`). Ngoại lệ: `dto`, `id`, `url`, `api`, `http`, `jwt`.
- Không kèm số thứ tự / trạng thái (`service1`, `component_new`, `controller_final`).

## 2. Bảng tra nhanh

| Thành phần | Java | React/JS |
|---|---|---|
| Thư mục | `lowercase` | `camelCase` (module) / `PascalCase` (shared component) |
| File | `PascalCase.java` | `PascalCase.jsx` (component) / `camelCase.js` |
| Class / Component | `PascalCase` | `PascalCase` |
| Hàm / phương thức | `camelCase` | `camelCase` |
| Biến | `camelCase` | `camelCase` |
| Constant | `SCREAMING_SNAKE_CASE` | `SCREAMING_SNAKE_CASE` |
| CSS class | - | `kebab-case` |
| REST endpoint / route | `kebab-case`, danh từ số nhiều | - |

- **Boolean**: `is` / `has` / `can`.
- **State React**: `[noun, setNoun]`. **Handler**: tiền tố `handle` hoặc `on`.
- **Env var Vite**: `VITE_` + `SCREAMING_SNAKE_CASE`.

## 3. Backend (Java)

### Package
`lowercase`, dạng `com.elingo.<module>.<layer>`. Không camelCase hay gạch dưới (`speaking`, không phải `speakingRoom`).

### Hậu tố class

| Loại | Hậu tố | Ví dụ |
|---|---|---|
| Entity (JPA) | không hậu tố | `User`, `Lesson` |
| Repository | `Repository` | `UserRepository` |
| Service interface | `Service` | `LessonService` |
| Service impl | `ServiceImpl` | `LessonServiceImpl` |
| Controller | `Controller` | `LessonController` |
| DTO request | `Request` | `CreateLessonRequest` |
| DTO response | `Response` | `LessonResponse` |
| Exception | `Exception` | `UserNotFoundException` |
| Enum | không hậu tố | `UserRole` |
| Configuration | `Config` | `SecurityConfig` |
| Filter / Interceptor | `Filter` / `Interceptor` | `JwtAuthFilter` |

Không dùng DTO chung chung (`AuthDTO`, `LessonDTO`) — luôn tách `Request` / `Response`.

### Tiền tố phương thức CRUD

`create` (tạo) · `get` / `find` (lấy một) · `getAll` / `findAll` (lấy danh sách) · `update` (sửa) · `delete` (xóa) · `exists` / `is` (kiểm tra tồn tại).

### REST endpoint
`kebab-case`, danh từ số nhiều, không dùng động từ trong URL (hành động thể hiện qua HTTP method): `/lessons`, `/lessons/{id}`, `/user-card-states`.

### Cấu trúc module
`com/elingo/<module>/` gồm: `controller/`, `service/` (interface + impl), `repository/`, `entity/`, `dto/` (`request/` + `response/`).

## 4. Frontend (React + JS)

### File không phải component

| Loại | Quy tắc | Ví dụ |
|---|---|---|
| API call / service | `camelCase` + hậu tố `Api` | `authApi.js` |
| Utility / helper | `camelCase` | `formatDate.js` |
| Hook tùy chỉnh | `camelCase` + tiền tố `use` | `useAuth.js` |
| Context | `PascalCase` + hậu tố `Context` | `AuthContext.jsx` |
| Constant | `camelCase` | `constants.js` |

File component `.jsx` dùng `PascalCase` và khớp tên component được export.

### Cấu trúc module
`frontend/src/modules/<module>/` gồm: `components/`, `pages/`, `<module>Api.js`, `index.js` (tùy chọn).
