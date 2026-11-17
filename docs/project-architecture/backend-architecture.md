# Kiến trúc Backend — Spring Boot

Tài liệu tổng quan về kiến trúc và cấu trúc tổ chức mã nguồn backend của hệ thống Elingo. Hệ thống được tổ chức theo kiến trúc phân tầng kết hợp hướng tính năng (Package by Feature / Module), giao tiếp liên module chủ yếu thông qua Spring Domain Events.

```
com.elingo
│
├── ElingoApplication.java
├── config/
├── common/
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
├── file/
├── notification/
└── gamification/
```

---

## 1. `config/`
Chứa các lớp cấu hình áp dụng toàn hệ thống:
- `SecurityConfig`: Cấu hình Spring Security, phân quyền Stateless qua JWT, CORS, Method Security (`@EnableMethodSecurity`), bỏ chặn các public endpoints.
- `JwtAuthenticationFilter`, `IpRateLimitFilter`, `RequestLoggingFilter`: Bộ lọc xác thực token, giới hạn tần suất truy cập theo IP (Bucket4j/Redis) và ghi log request (MDC).
- `WebSocketConfig`: Cấu hình STOMP over WebSocket cho đấu trường từ vựng và phòng thoại.
- `AsyncConfig`, `SchedulerConfig`: Cấu hình ThreadPool xử lý bất đồng bộ và tác vụ định kỳ.
- `R2Config`: Cấu hình client kết nối Cloudflare R2 (S3-compatible API).
- `WebMvcConfig`, `DatabaseInitializer`: Cấu hình chuyển đổi dữ liệu và khởi tạo dữ liệu nền.

---

## 2. `common/`
Chứa các thành phần hạ tầng và tiện ích dùng chung cho toàn dự án:
- `dto/`: Chuẩn hóa dữ liệu trả về gồm `ApiResponse<T>`, `PageResponse<T>`, `CursorResponse<T>`, `ErrorDetail`.
- `entity/`: `BaseEntity` (tự động quản lý `createdAt`, `updatedAt`).
- `enums/`: Các enum chung như `MediaKind`, `OtpType`, `RateLimitRule`.
- `event/`: Các domain events liên module: `FileAttachedEvent`, `FileDeletedEvent`, `LevelUpEvent`, `NotificationEvent`, `PaymentSucceededEvent`, `PostModeratedEvent`, `XpGainedEvent`.
- `exception/`: Bộ xử lý lỗi tập trung (`GlobalExceptionHandler`, `HttpExceptionHandler`, `WsExceptionHandler`) và định nghĩa lỗi chuẩn (`AppException`, `AppError`).
- `service/`: Các dịch vụ nền tảng dùng chung: `RedisService`, `EmailService`, `OtpService`, `RateLimiterService`.
- `websocket/`: Cấu trúc phong bì gửi tin WebSocket (`WsEventEnvelope`, `AbstractWsSender`).

---

## 3. `auth/`
Chịu trách nhiệm xác thực và định danh người dùng:
- **Chức năng chính:** Đăng ký tài khoản, gửi và xác thực mã OTP qua email, đăng nhập bằng mật khẩu hoặc Google OAuth2, cấp/làm mới JWT token, đặt lại mật khẩu.
- **Thành phần cốt lõi:**
  - `AuthenticationController`: Cung cấp các REST endpoint xác thực.
  - `AuthService`, `JwtService`, `GoogleTokenVerifierService`: Xử lý sinh mã JWT, xác thực token Google và kiểm tra đăng nhập.
  - Các custom validator (`@ValidPassword`, `@ValidUsername`).

---

## 4. `user/`
Quản lý thông tin tài khoản và hồ sơ người dùng:
- **Chức năng chính:** Xem và cập nhật hồ sơ cá nhân (tên hiển thị, avatar), đổi mật khẩu, đổi email xác thực qua OTP; cung cấp thông tin người dùng cho Security Context.
- **Thành phần cốt lõi:**
  - `User`: Entity người dùng trung tâm với phân quyền `Role` (`USER`, `ADMIN`).
  - `UserController`, `UserService`: Xử lý nghiệp vụ người dùng.
  - `CustomUserDetailsService`: Tích hợp Spring Security UserDetailsService.

---

## 5. `admin/`
Module dành riêng cho người quản trị hệ thống:
- **Mục đích:** Tách biệt quyền truy cập (`@PreAuthorize("hasRole('ADMIN')")`) và các thao tác quản trị khỏi các luồng người dùng thông thường, thao tác trên dữ liệu của các module nghiệp vụ tương ứng.
- **Phạm vi quản trị:**
  - Người dùng: Xem danh sách, kích hoạt/khóa tài khoản, cấp quyền.
  - Nội dung học tập: Quản lý và duyệt bộ từ vựng, quản lý bài học video, phân đoạn, transcript.
  - Kiểm duyệt cộng đồng: Giám sát bài viết, bình luận, lịch sử kiểm duyệt tự động.
  - Xử lý báo cáo vi phạm: Tiếp nhận, đánh giá và thực thi biện pháp xử lý báo cáo.
  - Gói cước và giao dịch: Quản lý gói Premium, tra cứu giao dịch nạp tiền.
  - Báo cáo thống kê: Dashboard tổng hợp người dùng, doanh thu và tương tác học tập.

---

## 6. `vocabulary/`
Quản lý kho từ vựng và flashcard:
- **Chức năng chính:** Quản lý bộ từ vựng (`Deck`), chủ đề (`Topic`), thẻ từ vựng (`Card`), phát âm ngữ âm (`CardPhonetic`), cấp độ (`CefrLevel`), nhãn (`Tag`). Cho phép tìm kiếm, lọc bộ từ vựng công khai hoặc cá nhân theo tiêu chí qua JPA Specification.
- **Thành phần cốt lõi:**
  - `CardController`, `DeckController`, `TopicController`, `CefrLevelController`, `TagController`.
  - `DeckAccessPolicy`: Kiểm soát quyền truy cập và chỉnh sửa bộ từ (hệ thống/cá nhân).
  - `DeckSpecifications`: Xây dựng truy vấn động cho bộ lọc tìm kiếm.

---

## 7. `learning/`
Bao gồm các chế độ học tập đa dạng, sử dụng dữ liệu từ bài học hoặc bộ từ:

### 7.1. `learning/flashcard/`
- **Chức năng:** Ôn tập từ vựng bằng thuật toán lặp lại ngắt quãng Spaced Repetition (SM-2).
- **Thành phần:** `FlashcardController`, `SpacedRepetitionService`, `CardStateService`, `UserCardStateRepository`. Tính toán khoảng thời gian lặp lại và cập nhật hệ số độ khó dựa trên đánh giá của người học.

### 7.2. `learning/lesson/`
- **Chức năng:** Học qua video phân đoạn, gắn kèm transcript gốc, transcript chuẩn hóa và bản dịch.
- **Thành phần:** `LessonController`, `LessonService`, `LessonAccessService`, `LessonProgressService`. Quản lý tiến độ hoàn thành bài học và từng phân đoạn (`LessonSegment`).

### 7.3. `learning/dictation/`
- **Chức năng:** Luyện nghe chép chính tả theo từng phân đoạn video.
- **Thành phần:** `DictationController`, `DictationService`, `DictationScoringService`. So khớp nội dung người dùng nhập với transcript chuẩn để chấm điểm và cung cấp gợi ý.

### 7.4. `learning/shadowing/`
- **Chức năng:** Luyện nói nhại giọng theo phát âm của người bản xứ trong video.
- **Thành phần:** `ShadowingController`, `ShadowingService`, `ShadowingScoringService`. Lưu trữ bản ghi âm và đánh giá điểm phát âm.

---

## 8. `progress/`
Tổng hợp tiến độ học tập đa kỹ năng:
- **Chức năng:** Truy vấn và tổng hợp dữ liệu học tập từ các module `flashcard`, `dictation`, `shadowing`, `lesson` để phục vụ màn hình dashboard cá nhân (tổng số từ, tỷ lệ hoàn thành, thời gian học).
- **Thành phần:** `ProgressService` (thuần truy vấn và tổng hợp, không tự sở hữu bảng dữ liệu riêng).

---

## 9. `battle/`
Đấu trường từ vựng 1vs1 đối kháng thời gian thực:
- **Chức năng:** Ghép trận ngẫu nhiên qua hàng đợi hoặc tạo phòng thi đấu riêng bằng mã; thi đấu qua 10 vòng câu hỏi trắc nghiệm sinh từ kho từ vựng; tính điểm và phân định thắng thua; phát thưởng XP khi kết thúc.
- **Thành phần cốt lõi:**
  - `BattleController`: REST API tra cứu lịch sử trận đấu và thống kê cá nhân.
  - `BattleSocketController`: Tiếp nhận các thao tác qua STOMP WebSocket (`/app/battle/*`).
  - `BattleEventPublisher`: Gửi sự kiện thời gian thực qua kênh riêng tư `/user/queue/battle`.
  - `BattleEngineService`, `BattleQueueService`, `BattleRoomService`: Điều phối trận đấu, hàng đợi và phòng chờ in-memory.
  - Cơ chế Grace Period (15 giây) xử lý người chơi ngắt kết nối và hỗ trợ vào lại trận (rejoin).

---

## 10. `speaking/`
Phòng luyện nói trực tuyến thời gian thực:
- **Chức năng:** Quản lý phòng luyện nói theo chủ đề và phiên tham gia của người dùng (`SpeakingRoom`, `SpeakingParticipant`), hỗ trợ điều phối tín hiệu WebRTC cho đàm thoại âm thanh.

---

## 11. `premium/`
Quản lý gói dịch vụ nâng cao và thanh toán:
- **Chức năng:** Quản lý danh mục gói Premium (`PremiumPlan`), tạo liên kết thanh toán VietQR qua cổng SePay, nhận và xác thực webhook giao dịch để tự động kích hoạt gói (`UserSubscription`); quét và vô hiệu hóa các gói đã hết hạn qua Scheduler.
- **Thành phần cốt lõi:**
  - `PremiumController`, `PaymentWebhookController`.
  - `SepayGateway`, `GatewayRegistry`: Tích hợp và xác thực chữ ký cổng thanh toán.
  - `PremiumScheduler`: Định kỳ kiểm tra trạng thái hết hạn đăng ký.
  - `PremiumAccessService`: Kiểm tra quyền hạn Premium khi người dùng truy cập tính năng giới hạn.

---

## 12. `community/`
Mạng xã hội và diễn đàn học tập:
- **Chức năng:** Đăng bài viết kèm đa phương tiện (`Post`, `PostMedia`), bình luận (`Comment`), thả cảm xúc (`Like`), phân loại theo nhóm chuyên mục (`PostGroup`).
- **Thành phần kiểm duyệt (`community/moderation`):**
  - Tự động kiểm duyệt nội dung đa phương tiện qua pipeline nhiều tầng (`TextStage`, `ImageStage`, `VideoStage`).
  - Tích hợp mô hình AI kiểm duyệt văn bản và hình ảnh, tự động trích xuất frame từ video (`FfmpegFrameExtractor`).
  - Lưu vết lịch sử kiểm duyệt (`PostModerationLog`) và cập nhật trạng thái bài viết (`PostStatus`).

---

## 13. `report/`
Tiếp nhận và quản lý báo cáo vi phạm:
- **Chức năng:** Cho phép người dùng gửi báo cáo kèm ảnh minh chứng đối với nhiều loại đối tượng (`USER`, `POST`, `COMMENT`, `DECK`, `LESSON`).
- **Thành phần cốt lõi:**
  - `ReportController`, `ReportService`.
  - Chiến lược kiểm tra đối tượng (`ReportChecker`): Tách biệt logic xác thực tính hợp lệ của từng loại mục tiêu (`PostReportChecker`, `CommentReportChecker`, `UserReportChecker`, `DeckReportChecker`, `LessonReportChecker`).

---

## 14. `notification/`
Hệ thống thông báo người dùng:
- **Chức năng:** Lưu trữ và hiển thị danh sách thông báo in-app, đánh dấu đã đọc, đếm số lượng thông báo chưa đọc.
- **Thành phần cốt lõi:**
  - `NotificationController`, `NotificationService`.
  - `NotificationEventListener`: Lắng nghe các domain events trong hệ thống để tự động tạo thông báo tương ứng cho người dùng.

---

## 15. `gamification/`
Hệ thống tạo động lực học tập:
- **Chức năng:** Tính điểm kinh nghiệm (`UserXp`), quản lý cấp độ (`Level`), duy trì chuỗi ngày học liên tục (`LearningStreak`), điểm danh nhận thưởng hàng ngày (`DailyCheckin`), quản lý huy hiệu (`Badge`) và lưu snapshot bảng xếp hạng (`LeaderboardSnapshot`).
- **Thành phần cốt lõi:**
  - `XpService`, `CheckInService`, `LevelCatalog`, `XpPolicy`.
  - `LearningXpListener`: Lắng nghe các domain event hoàn thành bài học, ôn từ, thắng battle để tự động cộng điểm kinh nghiệm và cập nhật chuỗi học.

---

## 16. `file/`
Quản lý tệp đa phương tiện trên Cloudflare R2:
- **Chức năng:** Cấp Presigned URL cho client tải file trực tiếp lên R2; xác thực định dạng file qua magic bytes tại server; quản lý vòng đời tệp thông qua các vùng lưu trữ (`staging`, `verified`, `uploads`).
- **Thành phần cốt lõi:**
  - `FileController`, `R2Service`.
  - `FileSignatureVerifier`: Kiểm tra magic bytes của ảnh, audio, video để phòng ngừa giả mạo đuôi tệp.
  - `FileLifecycleListener`: Lắng nghe `FileAttachedEvent` và `FileDeletedEvent` để di chuyển file vào thư mục chính thức hoặc xóa tệp khi bản ghi bị gỡ bỏ.

---

## Nguyên tắc tổ chức mã nguồn

1. **Kiến trúc phân tầng trong từng module:**
   - `controller/`: Định nghĩa REST endpoints, kiểm tra hợp lệ dữ liệu đầu vào (`@Valid`), điều hướng sang tầng service.
   - `service/`: Chứa interface và class triển khai (`impl/`), thực thi logic nghiệp vụ, quản lý giao dịch (`@Transactional`).
   - `repository/`: Kế thừa `JpaRepository` hoặc `JpaSpecificationExecutor`, chịu trách nhiệm truy xuất dữ liệu.
   - `entity/`: Ánh xạ bảng cơ sở dữ liệu, kế thừa `BaseEntity`.
   - `dto/`: Tách biệt dữ liệu yêu cầu (`request/`) và phản hồi (`response/`), không trả trực tiếp entity ra ngoài API.
   - `mapper/`: Ánh xạ hai chiều giữa Entity và DTO (MapStruct).

2. **Giao tiếp liên module qua Domain Events:**
   - Các module nghiệp vụ ưu tiên giao tiếp bất đồng bộ hoặc lỏng lẻo thông qua Spring Domain Events trong package `common/event/` (như cộng điểm thưởng, gửi thông báo, di chuyển tệp tải lên) nhằm giảm thiểu phụ thuộc vòng (circular dependency) giữa các package.
