# Backend — Spring Boot

```
com.elingo
│
├── ElingoApplication.java
├── config/
├── common/
│   ├── dto/
│   ├── annotation/
│   ├── exception/
│   ├── event/
│   ├── service/
|   └── util/
├── auth/
├── user/
├── admin/
├── vocabulary/
├── learning/
│   ├── flashcard/
│   ├── lesson/
│   ├── dictation/
│   └── shadowing/
├── progress/
├── battle/
├── speaking/
├── premium/
├── community/
├── report/
├── notification/
└── gamification/
```

---

## 1. `config/`
**Ý nghĩa:** Chứa các lớp cấu hình (bean, tích hợp bên thứ ba) áp dụng toàn hệ thống.

**Chức năng gợi ý:**
- `WebConfig` — CORS, interceptor, resolver.
- `SecurityConfig` — cấu hình Spring Security, filter chain, phân quyền `USER`/`ADMIN`.
- `SwaggerConfig` (OpenAPI) — tài liệu API.
- `WebSocketConfig` — cấu hình STOMP/WebSocket cho đấu trường, phòng Speaking.
- `AsyncConfig`, `SchedulerConfig` — job định kỳ (kiểm tra hết hạn Premium, dọn phòng).
- `RedisConfig`, `CacheConfig` — cache dữ liệu tra cứu nhiều (bộ từ phổ biến, bảng xếp hạng).
- `S3Config`/`CloudinaryConfig` — lưu trữ ảnh đại diện, ảnh bài viết.
- `PaymentConfig` — cấu hình cổng thanh toán (VNPay/Momo/Stripe...).

---

## 2. `common/`
**Ý nghĩa:** Chứa các thành phần dùng chung cho toàn bộ dự án, không thuộc riêng một nghiệp vụ nào.

**Cấu trúc con:**
```
common/
├── dto/ApiResponse.java, PageResponse.java
├── exception/ (GlobalExceptionHandler, custom exceptions: ResourceNotFoundException, BusinessException...)
├── service/ (EmailService, RedisService)
|    └─ impl/ (EmailServiceImpl, RedisServiceImpl)
└── util/ (DateUtils, StringUtils, FileUtils)
```

```
common/event/
├── FlashcardReviewedEvent.java
├── LessonCompletedEvent.java
├── BattleWonEvent.java
└── UserLoggedInEvent.java   (dùng cho check-in)
```

**Mục đích:**
- flashcard/, battle/, dictation/... sau khi xử lý xong nghiệp vụ chính thì applicationEventPublisher.publishEvent(new FlashcardReviewedEvent(...))
- gamification/ (hoặc progress/) có các @EventListener/@Async @EventListener lắng nghe và cộng XP/streak tương ứng.
- `GlobalExceptionHandler` (`@RestControllerAdvice`): bắt exception toàn cục, trả về response lỗi đồng nhất — đáp ứng yêu cầu phi chức năng về xử lý/validate input.
- `ApiResponse<T>`, `PageResponse<T>`: chuẩn hóa format response và phân trang cho các API danh sách (bộ từ, bài học, lịch sử học...).
- service/: dùng để xử lý các nghiệp vụ dùng chung và các implementation của các service này.

---

## 3. `auth/` *(package core — có đủ 4 tầng)*
**Ý nghĩa:** Đăng ký, đăng nhập, đăng nhập Google, refresh token, quên/đặt lại mật khẩu.

```
auth/
├── controller/AuthController.java
├── service/AuthService.java, AuthServiceImpl.java
├── repository/ (nếu cần truy vấn riêng, thường dùng chung UserRepository)
├── dto/
│   ├── request/RegisterRequest.java, LoginRequest.java, RefreshTokenRequest.java
│   └── response/AuthResponse.java (accessToken, refreshToken, user info)
└── entity/ RefreshToken.java (nếu lưu refresh token trong DB)
```

**Chức năng:** đăng ký email/mật khẩu, xác thực email, đăng nhập Google OAuth2, cấp/làm mới token, đăng xuất (thu hồi refresh token), quên mật khẩu.

---

## 4. `user/` *(package core)*
**Ý nghĩa:** Hồ sơ cá nhân của người dùng (không phải nghiệp vụ học tập).

```
user/
├── controller/UserController.java
├── service/UserService.java, UserServiceImpl.java
├── repository/UserRepository.java
├── entity/User.java
└── dto/
    ├── request/UpdateProfileRequest.java, ChangePasswordRequest.java
    └── response/UserProfileResponse.java
```

**Chức năng:** xem/cập nhật tên hiển thị, ảnh đại diện, đổi mật khẩu; là entity trung tâm được tham chiếu bởi hầu hết các package khác (vocabulary, learning, community, report...).

---

## 5. `admin/`
**Ý nghĩa:** Các chức năng dành riêng cho quản trị viên, thao tác trên dữ liệu do các package khác quản lý.

**Chức năng gợi ý (thường gọi lại service của các package khác thay vì tạo entity riêng):**
- `AdminUserController/Service` — xem danh sách, khóa/mở khóa, reset mật khẩu người dùng.
- `AdminVocabularyController/Service` — duyệt/quản lý bộ từ vựng hệ thống.
- `AdminLessonController/Service` — CRUD bài học video, phân đoạn, transcript.
- `AdminPremiumController/Service` — quản lý gói Premium (giá, thời hạn, quyền lợi).
- `AdminReportController/Service` — tiếp nhận và xử lý báo cáo vi phạm.
- `AdminDashboardController/Service` — thống kê user, doanh thu, bài học/bộ từ phổ biến, xuất báo cáo.
- `AdminCommunityController/Service` — kiểm duyệt/ẩn/xóa bài viết vi phạm.

**Mục đích:** Gom toàn bộ nghiệp vụ "Bộ phận Admin" ở một chỗ, tách bạch quyền truy cập (`@PreAuthorize("hasRole('ADMIN')")`) khỏi API dành cho người dùng thường, dù dữ liệu (entity) vẫn thuộc sở hữu của package nghiệp vụ tương ứng (vocabulary, learning, premium...).

---

## 6. `vocabulary/` *(package core)*
**Ý nghĩa:** Quản lý bộ từ vựng (cá nhân và hệ thống) và thẻ từ vựng.

**Chức năng:** tạo/sửa/xóa/công khai bộ từ, chia chủ đề con, thêm/sửa/xóa thẻ từ, import hàng loạt từ file Excel .

---

## 7. `learning/`
**Ý nghĩa:** Nhóm các chế độ học tập, mỗi chế độ là 1 sub-package độc lập vì logic tính điểm khác nhau, nhưng dùng chung dữ liệu bài học/bộ từ.

### 7.1. `learning/flashcard/` *(package core)*
```
flashcard/
├── controller/FlashcardController.java
├── service/FlashcardService.java, SpacedRepetitionService.java (thuật toán SM-2)
├── repository/FlashcardReviewRepository.java
├── entity/FlashcardReview.java (cardId, userId, easeFactor, interval, dueDate, level SM-2)
└── dto/
    ├── request/SrsReviewRequest.java (Again/Hard/Good/Easy)
    └── response/DueCardResponse.java
```
**Chức năng:** hiển thị thẻ đến hạn ôn tập, nhận đánh giá 4 mức (Again/Hard/Good/Easy), `SpacedRepetitionService` áp dụng thuật toán SM-2 tính `interval`/`easeFactor`/`dueDate` tiếp theo.

### 7.2. `learning/lesson/` *(package core)*
```
lesson/
├── controller/LessonController.java
├── service/LessonService.java
├── repository/LessonRepository.java, LessonSegmentRepository.java
├── entity/Lesson.java (youtube link, tags, CEFR level), LessonSegment.java (start/end time, transcript gốc, transcript chuẩn hóa, bản dịch)
└── dto/ request/response tương ứng
```
**Chức năng:** admin nhập link YouTube, chia đoạn (segment) kèm transcript và bản dịch, gắn nhãn/CEFR để tìm kiếm.

### 7.3. `learning/dictation/`
```
dictation/
├── controller/DictationController.java
├── service/DictationService.java, DictationScoringService.java
├── repository/DictationProgressRepository.java
├── entity/DictationProgress.java (segmentId, userId, transcript nhập, điểm, trạng thái hint)
└── dto/...
```
**Chức năng:** người dùng nghe segment, gõ lại nội dung; `DictationScoringService` so khớp với transcript chuẩn để tính điểm, hỗ trợ gợi ý từ khó, lưu tiến độ theo từng segment.

### 7.4. `learning/shadowing/`
```
shadowing/
├── controller/ShadowingController.java
├── service/ShadowingService.java, PronunciationScoringService.java (tích hợp AI chấm phát âm)
├── repository/ShadowingRecordRepository.java
├── entity/ShadowingRecord.java (segmentId, userId, audio URL, điểm ngữ điệu/phát âm, best score)
└── dto/...
```
**Chức năng:** người dùng ghi âm đọc theo segment, so sánh với audio gốc (qua service/API AI bên ngoài) để chấm điểm, lưu lại bản ghi âm tốt nhất.

---

## 8. `progress/`
**Ý nghĩa:** Tổng hợp tiến độ học tập từ các package `flashcard`, `dictation`, `shadowing`, `lesson` để hiển thị dashboard. Package này chỉ **đọc** dữ liệu từ repository của các module học, không sở hữu entity nghiệp vụ nào của riêng nó ngoài các bảng tổng hợp/cache phục vụ hiển thị.

```
progress/
├── controller/ProgressController.java
├── service/ProgressService.java (tổng hợp dữ liệu từ các module học: flashcard, dictation, shadowing, lesson)
└── dto/response/DashboardResponse.java (tổng số từ đã học, số từ cần ôn hôm nay, % hoàn thành, biểu đồ hoạt động, tỷ lệ ghi nhớ)
```

**Chức năng:** `ProgressService` truy vấn trực tiếp các repository của `flashcard/`, `dictation/`, `shadowing/`, `lesson/` (hoặc lắng nghe cùng các event trong `common/event/` nếu muốn tránh gọi thẳng repository chéo package) để tính số liệu, không tính XP/level/streak — các chỉ số đó thuộc về `gamification/`.

**Mục đích:** Tránh việc mỗi module học tự tính lại thống kê tổng quan; đây là lớp tổng hợp (aggregation) thuần thống kê, tách biệt rõ với `gamification/` (thưởng/động lực) dù cả hai cùng nghe chung nguồn sự kiện học tập.

---

## 9. `battle/`
**Ý nghĩa:** Đấu trường từ vựng 1vs1 theo thời gian thực.

```
battle/
├── controller/BattleController.java (REST: tạo phòng, lấy lịch sử)
├── websocket/BattleSocketHandler.java hoặc BattleWebSocketController.java (STOMP: đồng bộ câu hỏi, thời gian, điểm)
├── service/BattleMatchService.java (ghép trận ngẫu nhiên), BattleRoomService.java (tạo/tham gia phòng qua mã), BattleScoringService.java
├── repository/BattleMatchRepository.java, BattleRoomRepository.java
├── entity/BattleMatch.java, BattleRoom.java, BattleAnswer.java
└── dto/... (câu hỏi, trạng thái người chơi, kết quả)
```

**Chức năng:** ghép trận ngẫu nhiên hoặc tạo/tham gia phòng bằng mã; hỗ trợ 2 chế độ (chọn đáp án đúng / gõ từ vựng); đồng bộ trạng thái người chơi, câu hỏi, thời gian, điểm số theo thời gian thực qua WebSocket; tính kết quả dựa trên độ chính xác và thời gian trả lời.

---

## 10. `speaking/`
**Ý nghĩa:** Phòng luyện nói (voice chat) theo thời gian thực.

```
speaking/
├── controller/SpeakingRoomController.java (REST: tạo/tham gia/rời phòng bằng mã)
├── websocket/SpeakingSignalingHandler.java (tín hiệu WebRTC/trạng thái phòng qua WebSocket)
├── service/SpeakingRoomService.java (quản lý số lượng người tham gia, ghép ngẫu nhiên)
├── repository/SpeakingRoomRepository.java
├── entity/SpeakingRoom.java, SpeakingParticipant.java
└── dto/...
```

**Chức năng:** tạo phòng/nhận mã phòng, tham gia bằng mã hoặc ghép ngẫu nhiên, đồng bộ trạng thái tham gia/rời phòng, giới hạn số người theo cấu hình phòng; phần âm thanh thực tế thường qua WebRTC, package này chịu trách nhiệm signaling + quản lý trạng thái phòng.

---

## 11. `premium/`
**Ý nghĩa:** Gói Premium, đăng ký và thanh toán.

```
premium/
├── controller/PremiumPlanController.java, SubscriptionController.java, PaymentController.java (webhook)
├── service/PremiumPlanService.java, SubscriptionService.java, PaymentService.java
├── repository/PremiumPlanRepository.java, UserSubscriptionRepository.java, PaymentTransactionRepository.java
├── entity/PremiumPlan.java (tên, giá, thời hạn, quyền lợi), UserSubscription.java (trạng thái, ngày hết hạn), PaymentTransaction.java
└── dto/...
```

**Chức năng:** xem danh sách gói, đăng ký, thanh toán qua cổng thanh toán (webhook cập nhật kết quả), cập nhật trạng thái/thời hạn Premium, job định kỳ (`scheduler`) tự động vô hiệu hóa quyền khi hết hạn, `PremiumAccessAspect`/`Interceptor` kiểm tra quyền Premium khi truy cập chức năng liên quan.

---

## 12. `community/`
**Ý nghĩa:** Bài viết cộng đồng, bình luận, tương tác.

---

## 13. `report/`
**Ý nghĩa:** Báo cáo vi phạm (bài viết, người dùng, bộ từ vựng công khai).

```
report/
├── controller/ReportController.java (user gửi báo cáo), 
├── service/ReportService.java
├── repository/ReportRepository.java
├── entity/Report.java (loại đối tượng bị báo cáo, lý do, trạng thái: Chờ xử lý/Đã xử lý/Đã từ chối)
└── dto/...
```

**Chức năng:** người dùng gửi báo cáo kèm lý do (chọn từ danh mục), lưu trạng thái, admin xử lý (gỡ bài, khóa user...) và cập nhật trạng thái, có thể trigger `notification` gửi kết quả cho người báo cáo.

---

## 14. `notification/`
**Ý nghĩa:** Thông báo trong hệ thống (báo cáo mới cho admin, kết quả xử lý báo cáo cho user...).

```
notification/
├── controller/NotificationController.java
├── service/NotificationService.java
├── repository/NotificationRepository.java
├── entity/Notification.java
└── dto/...
```

---

## 15. `gamification/`
**Ý nghĩa:** Quản lý các cơ chế tạo động lực học tập: điểm kinh nghiệm (XP), cấp độ (level), điểm danh hằng ngày (check-in) và streak (chuỗi ngày học liên tiếp), cùng bảng xếp hạng. Package này không tự chứa logic nghiệp vụ học tập — nó chỉ **lắng nghe sự kiện** từ các module khác (`flashcard`, `dictation`, `shadowing`, `lesson`, `battle`, `auth`...) để cộng thưởng, tránh phụ thuộc ngược vào các module đó.

```
gamification/
├── controller/
│ ├── GamificationController.java (xem XP/level hiện tại, streak, check-in hôm nay)
│ └── LeaderboardController.java (bảng xếp hạng XP theo tuần/tháng)
├── service/
│ ├── XpService.java (cộng/trừ XP, tính level từ tổng XP)
│ ├── StreakService.java (cập nhật streak khi có hoạt động học trong ngày, reset khi bỏ ngày)
│ ├── CheckInService.java (điểm danh hằng ngày, thưởng XP theo mốc ngày)
│ └── LeaderboardService.java (đọc dữ liệu XP, cache Redis cho bảng xếp hạng)
├── listener/
│ ├── LearningActivityListener.java (@EventListener FlashcardReviewedEvent, LessonCompletedEvent, DictationCompletedEvent, ShadowingCompletedEvent → gọi XpService + StreakService)
│ └── BattleResultListener.java (@EventListener BattleWonEvent → cộng XP thắng trận)
├── repository/
│ ├── UserXpRepository.java
│ ├── UserLevelRepository.java
│ ├── LearningStreakRepository.java
│ └── DailyCheckInRepository.java
├── entity/
│ ├── UserXp.java (userId, tổng XP)
│ ├── UserLevel.java (userId, level hiện tại, XP cần cho level tiếp theo)
│ ├── LearningStreak.java (userId, streak hiện tại, streak dài nhất, ngày học gần nhất)
│ └── DailyCheckIn.java (userId, ngày check-in, XP/phần thưởng nhận được)
└── dto/
├── request/CheckInRequest.java
└── response/GamificationSummaryResponse.java (XP, level, streak, đã check-in hôm nay chưa), LeaderboardEntryResponse.java  
```


**Chức năng:**
- **XP:** mỗi hành động học tập hợp lệ (ôn flashcard đúng, hoàn thành lesson/segment dictation-shadowing, thắng battle...) phát sinh Event tương ứng trong `common/event/`; `LearningActivityListener`/`BattleResultListener` bắt sự kiện, gọi `XpService.addXp(userId, amount, source)`, đồng thời kiểm tra ngưỡng lên cấp và cập nhật `UserLevel`.
- **Streak:** cùng các listener trên gọi `StreakService` — có hoạt động hợp lệ trong ngày thì tăng streak, bỏ lỡ 1 ngày thì reset về 0, đồng thời lưu streak dài nhất từng đạt.
- **Điểm danh (check-in):** API riêng cho phép người dùng chủ động bấm điểm danh mỗi ngày (độc lập việc có học hay không), thưởng XP tăng dần theo số ngày liên tiếp, lưu lịch sử vào `DailyCheckIn`.
- **Bảng xếp hạng:** `LeaderboardService` tổng hợp XP theo tuần/tháng, dùng cache Redis (`config/RedisConfig`) để tránh truy vấn nặng.

**Mục đích:** Tách hoàn toàn phần "thưởng/động lực" khỏi phần "thống kê" (`progress/`), giao tiếp với các module học tập/battle qua **event-driven** (`common/event/` + `@EventListener`) — thêm nguồn cộng XP mới hoặc đổi công thức tính level chỉ cần sửa trong `gamification/`, không ảnh hưởng `progress/` hay các module học khác.

---

## Nguyên tắc phân tầng trong mỗi package core

- **controller/**: nhận request, validate (`@Valid`), gọi service, không chứa logic nghiệp vụ.
- **service/**: chứa interface + implementation, xử lý toàn bộ logic nghiệp vụ, transaction (`@Transactional`).
- **repository/**: interface `JpaRepository`/`JpaSpecificationExecutor`, chỉ chứa truy vấn dữ liệu.
- **entity/**: ánh xạ bảng DB (`@Entity`), kế thừa `common.entity.BaseEntity`.
- **dto/**: tách `request/` và `response/`, không tái sử dụng entity làm response để tránh lộ dữ liệu và dễ tuỳ biến theo từng API.