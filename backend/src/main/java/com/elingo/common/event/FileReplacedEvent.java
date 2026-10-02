package com.elingo.common.event;

/**
 * Một file của người dùng vừa được thay bằng file mới.
 *
 * <p>Module sở hữu dữ liệu phát event này sau khi đã cập nhật xong trường file trong
 * entity của mình — ví dụ {@code user} đổi ảnh đại diện. Module đó biết chính xác
 * {@code oldFileKey} là gì, còn module {@code file} thì không có cách nào đoán: key là
 * UUID nên không tồn tại khái niệm "cùng tên" để dò.
 *
 * <p>Module {@code file} nghe event này để xoá {@code oldFileKey} — xoá sau khi
 * transaction commit, xem {@code FileCleanupListener}.
 *
 * @param oldFileKey  key cần xoá; có thể {@code null} nếu trước đó chưa có file
 * @param newFileKey  key vừa ghi, giữ lại để log đầy đủ
 */
public record FileReplacedEvent(String oldFileKey, String newFileKey) {
}
