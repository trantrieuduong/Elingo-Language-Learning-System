package com.elingo.file.listener;

import com.elingo.common.event.FileAttachedEvent;
import com.elingo.common.event.FileDeletedEvent;
import com.elingo.common.exception.AppException;
import com.elingo.file.service.R2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "FILE-LIFECYCLE-LISTENER")
public class FileLifecycleListener {

    private final R2Service r2Service;

    /** File đã được một bản ghi tham chiếu, nên nó rời vùng chờ sang vùng vĩnh viễn. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFileAttached(FileAttachedEvent event) {
        String verifiedKey = event.verifiedFileKey();
        String uploadsKey = event.uploadsFileKey();
        if (verifiedKey.isBlank() || uploadsKey.isBlank()) {
            return; // Bản ghi vốn không đính kèm file nào.
        }

        log.info("File attached verifiedKey={} uploadsKey={}", verifiedKey, uploadsKey);
        try {
            r2Service.promoteToUploads(verifiedKey, uploadsKey);
        } catch (AppException e) {
            // R2ServiceImpl đã ghi lỗi copy kèm chi tiết; ở đây chỉ chặn nó không chạy tiếp.
            log.debug("File promotion stopped, already reported uploadsKey={}", uploadsKey);
        } catch (Exception e) {
            log.warn("File promotion skipped verifiedKey={} uploadsKey={}", verifiedKey, uploadsKey, e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFileDeleted(FileDeletedEvent event) {
        if (event.fileKeys().isEmpty()) {
            return; // Bản ghi vốn không có file nào đính kèm.
        }

        log.info("Files deleted count={}", event.fileKeys().size());
        safelyDeleteAll(event.fileKeys());
    }

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