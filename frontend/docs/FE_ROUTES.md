# FE_ROUTES.md — Elingo Frontend Route Structure

> Tài liệu này định nghĩa toàn bộ route structure của Elingo Frontend.
> Custom router được implement trong `src/App.jsx` (không dùng React Router).
> Cập nhật file này mỗi khi thêm route mới.
>
> **Nguồn kiểm chứng:** route chỉ được xem là hiện tại khi đã có trong `PUBLIC_PATHS`/`PRIVATE_USER_PATHS` và `renderContent()` của `src/App.jsx`. Route dự kiến phải ghi rõ là planned, không đưa vào guard trước khi có page.

---

## 1. Route Map Hiện Tại

> **Trạng thái implementation:** Landing và bốn auth routes ở mục 1.1 đã có page thực. Các private/admin route bên dưới hiện được guard nhưng đa số vẫn render `PlaceholderPage` trong `App.jsx`; tên component trong bảng là target khi module tương ứng được triển khai.

### 1.1 Public Routes (Không cần đăng nhập)

| Path | Component | Module | Ghi chú |
|---|---|---|---|
| `/` | `LandingPage` | `auth/pages/` | Trang chủ |
| `/login` | `LoginPage` | `auth/pages/` | Đăng nhập |
| `/signup` | `SignupPage` | `auth/pages/` | Đăng ký |
| `/account-verification` | `AccountVerificationPage` | `auth/pages/` | Xác thực email bằng OTP; nhận email qua history state |

**Planned, chưa tạo route/page:** `/forgot-password`, `/reset-password`.

### 1.2 Private User Routes (Cần đăng nhập — role: USER)

| Path | Component | Module | Ghi chú |
|---|---|---|---|
| `/dashboard` | `DashboardPage` | `user/pages/` | Trang chính sau đăng nhập |
| `/learning` | `LearningHubPage` | `learning/lesson/pages/` | Hub học tập |
| `/learning/lesson/:lessonId` | `LessonDetailPage` | `learning/lesson/pages/` | Chi tiết bài học |
| `/learning/dictation/:lessonId` | `DictationPage` | `learning/dictation/pages/` | Nghe chép chính tả |
| `/learning/shadowing/:lessonId` | `ShadowingPage` | `learning/shadowing/pages/` | Luyện nói đuổi |
| `/flashcard` | `FlashcardListPage` | `learning/flashcard/pages/` | Danh sách deck |
| `/flashcard/:deckId` | `FlashcardStudyPage` | `learning/flashcard/pages/` | Học flashcard |
| `/flashcard/:deckId/review` | `FlashcardReviewPage` | `learning/flashcard/pages/` | Ôn tập flashcard |
| `/vocabulary` | `VocabularyListPage` | `vocabulary/pages/` | Danh sách bộ từ vựng |
| `/vocabulary/:deckId` | `VocabularyDetailPage` | `vocabulary/pages/` | Chi tiết bộ từ vựng |
| `/battle` | `BattleLobbyPage` | `battle/pages/` | Sảnh chờ thi đấu |
| `/battle/play` | `BattlePlayPage` | `battle/pages/` | Màn hình thi đấu (WebSocket) |
| `/speaking` | `SpeakingLobbyPage` | `speaking/pages/` | Danh sách phòng luyện nói |
| `/speaking/:roomId` | `SpeakingRoomPage` | `speaking/pages/` | Phòng luyện nói (WebRTC) |
| `/community` | `CommunityFeedPage` | `community/pages/` | Feed cộng đồng |
| `/community/post/:postId` | `PostDetailPage` | `community/pages/` | Chi tiết bài viết |
| `/gamification` | `LeaderboardPage` | `gamification/pages/` | Bảng xếp hạng + XP |
| `/progress` | `ProgressPage` | `progress/pages/` | Tiến độ học tập cá nhân |
| `/premium` | `PremiumPage` | `premium/pages/` | Gói Premium + thanh toán |
| `/notification` | `NotificationPage` | _(chưa có module)_ | Xem ghi chú bên dưới |
| `/profile` | `ProfilePage` | `user/pages/` | Trang cá nhân (own) |
| `/profile/:userId` | `UserProfilePage` | `user/pages/` | Trang cá nhân người dùng khác |

### 1.3 Admin Routes (Cần đăng nhập — role: ADMIN)

| Path | Component | Module | Ghi chú |
|---|---|---|---|
| `/admin` | `AdminDashboardPage` | `admin/pages/dashboard/` | Dashboard admin |
| `/admin/users` | `AdminUserListPage` | `admin/pages/user/` | Quản lý người dùng |
| `/admin/users/:userId` | `AdminUserDetailPage` | `admin/pages/user/` | Chi tiết user |
| `/admin/lessons` | `AdminLessonListPage` | `admin/pages/lesson/` | Quản lý bài học |
| `/admin/lessons/create` | `AdminLessonCreatePage` | `admin/pages/lesson/` | Tạo bài học |
| `/admin/lessons/:lessonId/edit` | `AdminLessonEditPage` | `admin/pages/lesson/` | Sửa bài học |
| `/admin/flashcards` | `AdminFlashcardListPage` | `admin/pages/flashcard/` | Quản lý flashcard |
| `/admin/vocabulary` | `AdminVocabularyListPage` | `admin/pages/vocabulary/` | Quản lý từ vựng |
| `/admin/premium` | `AdminPremiumPage` | `admin/pages/premium/` | Quản lý gói Premium |
| `/admin/reports` | `AdminReportListPage` | `admin/pages/report/` | Xử lý báo cáo vi phạm |
| `/admin/community` | `AdminCommunityPage` | `admin/pages/community/` | Kiểm duyệt cộng đồng |

>  **Ghi chú module `notification`**: Module này được mô tả trong `frontend-architecture.md` nhưng chưa có folder trong skeleton hiện tại. Khi implement, tạo `src/modules/notification/` với cấu trúc chuẩn.

---

## 2. Route Structure Trong App.jsx

### 2.1 Simple Routes — switch/case

```jsx
switch (path) {
  case '/':      return <LandingPage onNavigate={navigate} />
  case '/login': return <LoginPage onNavigate={navigate} />
  // ...
}
```

### 2.2 Dynamic Routes — regex matching

```jsx
// Pattern: /path/:id
const match = path.match(/^\/flashcard\/([a-zA-Z0-9-]+)$/)
if (match) {
  return <FlashcardStudyPage deckId={match[1]} onNavigate={navigate} />
}
```

### 2.3 Prefix Routes — startsWith

```jsx
if (path.startsWith('/admin')) {
  return <AdminLayout onNavigate={navigate} currentPath={path} />
}
```

---

## 3. Route Guard Logic (trong App.jsx)

```
Loading → hiện app-loading-screen
Không có user + private route → navigate('/login')
User role ADMIN + non-admin path → navigate('/admin')
User role USER + /admin/* → navigate('/dashboard')
```

### 3.1 Public Paths (không redirect khi chưa đăng nhập)

```js
const PUBLIC_PATHS = [
  '/', '/login', '/signup', '/account-verification'
]
```

---

## 4. Query Params Convention

| Route | Query Param | Ví dụ | Dùng cho |
|---|---|---|---|
| Trang list | `?page=&limit=&q=` | `/learning?page=2&q=grammar` | Phân trang + tìm kiếm |

---

## 5. Quy Tắc Thêm Route Mới

### 5.1 Quy trình

1. **Thêm path và render page** vào đúng nhóm trong `renderContent()` của `App.jsx`
2. **Cập nhật `PUBLIC_PATHS`** nếu là public route
3. **Cập nhật `PRIVATE_USER_PATHS`** nếu là private route (để route guard biết)
4. **Tạo page component** trong đúng module: `modules/moduleName/pages/PageName.jsx`
5. **Tạo CSS file** cùng thư mục: `modules/moduleName/pages/PageName.css`
6. **Cập nhật file này** (FE_ROUTES.md) với route mới

### 5.2 Quy tắc path

- **Lowercase, kebab-case**: `/speaking-rooms` không phải `/SpeakingRooms`
- **Plural nouns** cho list: `/lessons`, `/flashcards`, `/users`
- **:param** cho dynamic segment — ID trong URL là UUID hoặc MongoDB ObjectId
- **Prefix theo domain**: `/admin/*` cho tất cả admin pages

### 5.3 Không được làm

- KHÔNG dùng React Router — thêm route bằng switch/case hoặc regex trong `renderRoute()`
- KHÔNG navigate trực tiếp trong component — luôn nhận `onNavigate` prop
- KHÔNG tạo route trùng lặp với route đã có

---

## 6. Layout Mapping

| Route nhóm | Layout | Header | Footer |
|---|---|---|---|
| Public routes (`/`, `/login`...) | Public |  `<Header>` |  `<Footer>` |
| Private user routes (`/dashboard`...) | Public |  `<Header>` |  `<Footer>` |
| Admin routes (`/admin/*`) | Placeholder tạm thời; dùng `<AdminLayout>` khi admin module được triển khai | Admin sidebar riêng |  |

---

## 7. Realtime Routes

Hai module dùng kết nối realtime cần context riêng:

| Route | Protocol | Context |
|---|---|---|
| `/battle/play` | WebSocket (Socket.io) | `BattleSocketContext` |
| `/speaking/:roomId` | WebRTC + WebSocket signaling | `SpeakingRoomContext` _(chưa tạo)_ |

> Context của battle và speaking nên wrap chỉ các route cần dùng, không wrap toàn app.
