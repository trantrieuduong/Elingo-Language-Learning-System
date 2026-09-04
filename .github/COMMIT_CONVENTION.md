# Commit Convention — Elingo

Dự án dùng [Conventional Commits](https://www.conventionalcommits.org/) để chuẩn hóa message commit, hỗ trợ sinh changelog tự động và đọc lịch sử dễ dàng.

## 1. Cấu trúc message

```
<type>(<scope>): <description>

<body>

<footer>
```

- **Dòng đầu** (`type(scope): description`) là bắt buộc, tối đa ~72 ký tự.
- **body** và **footer** là tùy chọn, cách dòng đầu một dòng trống.
- Nội dung message viết bằng **tiếng Anh**, thể mệnh lệnh hiện tại: "add", "fix" (không dùng "added", "adds", "fixing").

## 2. Type

| Type | Ý nghĩa |
|---|---|
| `feat` | Thêm tính năng mới |
| `fix` | Sửa lỗi |
| `docs` | Chỉ thay đổi tài liệu |
| `style` | Format code, không đổi logic (khoảng trắng, dấu `;`, format...) |
| `refactor` | Viết lại code, không thêm tính năng cũng không sửa lỗi |
| `perf` | Cải thiện hiệu năng |
| `test` | Thêm hoặc sửa test |
| `build` | Hệ thống build, thư viện phụ thuộc (Maven, npm...) |
| `ci` | Cấu hình CI/CD (GitHub Actions...) |
| `chore` | Việc vặt khác, không đụng `src` hay `test` |
| `revert` | Hoàn tác một commit trước đó |

## 3. Scope

Scope là package/module bị ảnh hưởng, đặt trong ngoặc đơn. Bỏ scope nếu thay đổi trải rộng nhiều nơi.

**Theo package:** `backend`, `frontend`, `ai-service`, `docs`

**Theo module nghiệp vụ** (dùng chung backend & frontend):
`auth`, `user`, `admin`, `vocabulary`, `learning`, `flashcard`, `lesson`, `dictation`, `shadowing`, `progress`, `battle`, `speaking`, `premium`, `community`, `report`, `notification`, `gamification`

**Khác:** `config`, `common`, `deps`

## 4. Description

- Chữ đầu viết thường, **không** có dấu chấm cuối.
- Ngắn gọn, mô tả hành động: `add login endpoint`, `fix streak reset on missed day`.

## 5. Body

- Giải thích **what** và **why**, không mô tả **how** (code đã thể hiện).
- Mỗi dòng wrap ~72 ký tự.

## 6. Footer

- Liên kết issue: `Closes #123`, `Refs #45`.
- **Breaking change**: thêm `!` ngay sau `type(scope)` và/hoặc dòng `BREAKING CHANGE:` ở footer.

```
feat(auth)!: replace session cookie with JWT

BREAKING CHANGE: clients must send Authorization: Bearer <token>.
Closes #88
```

## 7. Ví dụ

```
feat(flashcard): add SM-2 spaced repetition service
fix(battle): prevent negative score on timeout
refactor(backend): split LessonDTO into request and response
perf(gamification): cache leaderboard with Redis
test(dictation): cover scoring service edge cases
docs(readme): add ai-service setup instructions
build(frontend): upgrade vite to v8
chore(deps): bump spring-boot to 4.1.1
```
