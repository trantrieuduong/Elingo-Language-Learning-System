# Quy tắc vòng đời và liên kết dữ liệu Card (Card Lifecycle Rules) — Elingo

Tài liệu này quy định chi tiết về mối quan hệ liên kết dữ liệu giữa **Deck**, **Topic** và **Card**, cơ chế chuyển đổi trạng thái khi xóa dữ liệu, cũng như các ràng buộc toàn vẹn cần tuân thủ trong toàn bộ hệ thống Elingo.

---

## 1. Quy tắc bất biến (Core Invariant)

Mọi bản ghi trong bảng `cards` bắt buộc chỉ tồn tại ở một trong **hai trạng thái duy nhất**:

1. **Trạng thái hợp lệ (Active)**: Có đầy đủ cả `deck_id` và `topic_id` (`deck_id IS NOT NULL AND topic_id IS NOT NULL`). Thẻ luôn thuộc về một chủ đề cụ thể nằm trong một bộ từ vựng xác định.
2. **Trạng thái mồ côi (Orphaned)**: Không có cả hai khóa ngoại (`deck_id IS NULL AND topic_id IS NULL`). Thẻ không còn thuộc về bất kỳ bộ từ vựng hay chủ đề nào.

> **Tuyệt đối cấm**: Trạng thái "nửa mồ côi" (có `deck_id` nhưng `topic_id = null`, hoặc có `topic_id` nhưng `deck_id = null`).

---

## 2. Cơ chế xử lý khi xóa dữ liệu

### 2.1 Khi xóa cứng User Deck
- **Thao tác**: Người dùng xóa một bộ từ vựng của mình qua endpoint `DELETE /decks/{deckId}`.
- **Luồng xử lý**:
  1. Service xác thực quyền sở hữu (`DeckAccessPolicy.assertCanEdit`).
  2. Thực hiện lệnh xóa Deck trong database: `deckRepository.deleteById(deckId)`.
  3. **Tầng cơ sở dữ liệu tự động kích hoạt**:
     - Bảng `topics` có khóa ngoại `FOREIGN KEY (deck_id) REFERENCES decks(id) ON DELETE CASCADE` -> Toàn bộ topics thuộc deck bị xóa cứng.
     - Bảng `cards` có `FOREIGN KEY (deck_id) REFERENCES decks(id) ON DELETE SET NULL` -> Cột `deck_id` của các card chuyển thành `NULL`.
     - Đồng thời, khi các topics bị xóa theo cascade, khóa ngoại `FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE SET NULL` của `cards` tiếp tục kích hoạt -> Cột `topic_id` chuyển thành `NULL`.
  4. **Kết quả**: Toàn bộ cards thuộc deck đó chuyển thành **mồ côi** (`deck_id = null, topic_id = null`).

### 2.2 Khi xóa cứng Topic (User Topic & System Topic)
- **Thao tác**: Xóa một chủ đề qua endpoint `DELETE /topics/{topicId}`.
- **Luồng xử lý**:
  1. Service xác thực quyền sở hữu (`assertCanEditByTopic`).
  2. **Tách thẻ chủ động trước khi xóa topic**:
     Gọi `cardRepository.detachByTopicId(topicId)` thực thi câu lệnh:
     ```sql
     UPDATE cards SET deck_id = NULL, topic_id = NULL WHERE topic_id = :topicId;
     ```
     *Lý do*: Nếu chỉ xóa topic dựa vào database `ON DELETE SET NULL`, MySQL chỉ set `topic_id = NULL` trong khi `deck_id` vẫn còn giá trị (vi phạm quy tắc bất biến). Do đó, ứng dụng chủ động set cả hai khóa ngoại về `NULL`.
  3. Xóa topic: `topicRepository.deleteById(topicId)`.
  4. Đồng bộ biến đếm trên Deck: Giảm `topic_count` đi 1 và giảm `card_count` tương ứng với số lượng card vừa bị tách.

### 2.3 Đối với System Deck (Module Admin phát triển sau)
- **Quy tắc**: System Deck **không bao giờ được phép xóa cứng**, chỉ được phép chuyển trạng thái (`status = ARCHIVED`).
- **Ngoại lệ với System Topic**: Trường hợp một chủ đề trong System Deck bị xóa cứng, hệ thống vẫn áp dụng quy trình gọi `detachByTopicId` để đưa toàn bộ cards trong topic đó về trạng thái mồ côi (`deck_id = null, topic_id = null`).

---

## 3. Hành vi và vòng đời của Card mồ côi

1. **Bảo toàn dữ liệu học tập**:
   - Khi một bộ thẻ hoặc chủ đề bị xóa, thẻ biến thành mồ côi nhưng **không bị xóa cứng ngay khỏi bảng `cards`**.
   - Mục đích: Bảo toàn lịch sử học tập và thuật toán lặp lại ngắt quãng SM-2 của người dùng trong bảng `user_card_states` (`srs_ease_factor`, `srs_interval`, `srs_next_review_at`).
2. **Ngăn chặn truy cập và học mới**:
   - Khi truy vấn thông tin hoặc thao tác học tập, nếu phát hiện `card.deck == null`, hệ thống lập tức chặn truy cập và ném ngoại lệ `AppError.CARD_NOT_AVAILABLE` hoặc `AppError.CARD_NOT_FOUND`.
   - Card mồ côi không xuất hiện trong bất kỳ kết quả tìm kiếm, danh sách chủ đề hay bài học mới nào.
3. **Cơ chế dọn dẹp định kỳ (Scheduled Cleanup)**:
   - Hệ thống chạy tác vụ ngầm định kỳ (cron job) để quét bảng `cards`.
   - Những card mồ côi (`deck_id IS NULL AND topic_id IS NULL`) mà **không còn bất kỳ bản ghi nào trong `user_card_states`** sẽ được xóa cứng khỏi database kèm sự kiện xóa file ảnh (`image_url`) trên S3/MinIO để giải phóng dung lượng.

---

## 4. Ràng buộc toàn vẹn đề xuất ở tầng Cơ sở dữ liệu (Database Constraint)

Để bảo đảm quy tắc bất biến hoạt động an toàn ngay cả khi có thao tác truy vấn trực tiếp bằng tay từ phía database administrator, bảng `cards` được khuyến nghị bổ sung ràng buộc kiểm tra (CHECK constraint — hỗ trợ từ MySQL 8.0.16+):

```sql
ALTER TABLE cards
ADD CONSTRAINT chk_card_deck_topic_consistency
CHECK (
  (deck_id IS NULL AND topic_id IS NULL)
  OR
  (deck_id IS NOT NULL AND topic_id IS NOT NULL)
);
```
Ràng buộc này bảo đảm rằng bất kỳ câu lệnh SQL nào tạo ra trạng thái lệch (chỉ có 1 trong 2 khóa ngoại) đều sẽ bị từ chối ở tầng database engine.

