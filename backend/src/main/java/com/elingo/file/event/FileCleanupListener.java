package com.elingo.file.event;

import com.elingo.common.event.FileDeletedEvent;
import com.elingo.common.event.FileReplacedEvent;
import com.elingo.file.service.R2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Dọn file trên R2 khi module sở hữu dữ liệu báo file không còn được dùng nữa.
 *
 * <p>Thay vì module {@code file} tự đoán file nào là file cũ — điều không làm được vì
 * key là UUID, mỗi file một tên riêng — module sở hữu ({@code user}, {@code community}…)
 * phát event ngay sau khi cập nhật entity. Module đó có sẵn cả hai key.
 */
@Component
@RequiredArgsConstructor
@Slf4j(topic = "FILE-CLEANUP-LISTENER")
public class FileCleanupListener {

    private final R2Service r2Service;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFileReplaced(FileReplacedEvent event) {
        String oldFileKey = event.oldFileKey();
        if (oldFileKey == null || oldFileKey.isBlank()) {
            return; // Lần đầu gán file, không có bản cũ để xoá.
        }
        log.info("File replaced oldKey={} newKey={}", oldFileKey, event.newFileKey());
        safelyDeleteAll(List.of(oldFileKey));
    }

    /**
     * Một bản ghi có thể tham chiếu nhiều file (bài viết có nhiều ảnh), nên một lần commit
     * có thể cần dọn cả danh sách — đây là lý do {@code FileDeletedEvent} mang
     * {@code List<String>} thay vì một key.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFileDeleted(FileDeletedEvent event) {
        if (event.fileKeys().isEmpty()) {
            return; // Bản ghi vốn không có file nào đính kèm.
        }
        log.info("Files deleted count={}", event.fileKeys().size());
        safelyDeleteAll(event.fileKeys());
    }

    /**
     * Listener chạy sau khi request đã trả kết quả cho client, nên lỗi ở đây không có ai
     * nhận. Nuốt lỗi lại giữ đúng nguyên tắc của module: dọn file là việc phụ, không
     * được làm hỏng thao tác chính. R2 vẫn tính phí theo dung lượng đang dùng, nên file
     * sót lại sẽ được dọn bởi quy tắc vòng đời hoặc lần dọn thủ công sau.
     *
     * <p>Bắt lỗi quanh từng key chứ không quanh cả vòng lặp: một key hỏng không được làm
     * các key còn lại không được dọn.
     */
    private void safelyDeleteAll(List<String> fileKeys) {
        for (String fileKey : fileKeys) {
            try {
                r2Service.deleteFile(fileKey);
            } catch (Exception e) {
                log.warn("File cleanup skipped reason=deleteFailed fileKey={}", fileKey, e);
            }
        }
    }
}
