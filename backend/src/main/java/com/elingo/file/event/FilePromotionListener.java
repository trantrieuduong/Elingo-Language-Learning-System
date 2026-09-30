package com.elingo.file.event;

import com.elingo.common.event.FileAttachedEvent;
import com.elingo.file.service.R2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Chuyển file từ vùng chờ {@code verified/} sang vùng vĩnh viễn {@code uploads/} khi module
 * sở hữu dữ liệu đã thật sự tham chiếu tới nó.
 */
@Component
@RequiredArgsConstructor
@Slf4j(topic = "FILE-PROMOTION-LISTENER")
public class FilePromotionListener {

    private final R2Service r2Service;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFileAttached(FileAttachedEvent event) {
        String verifiedKey = event.verifiedFileKey();
        String uploadsKey = event.uploadsFileKey();
        if (verifiedKey.isBlank() || uploadsKey.isBlank()) {
            return; // Bản ghi vốn không đính kèm file nào.
        }

        log.info("File attached. verifiedKey={}, uploadsKey={}", verifiedKey, uploadsKey);
        try {
            r2Service.promoteToUploads(verifiedKey, uploadsKey);
        } catch (Exception e) {
            log.error("Failed to promote file. verifiedKey={}, uploadsKey={}", verifiedKey, uploadsKey, e);
        }
    }
}
