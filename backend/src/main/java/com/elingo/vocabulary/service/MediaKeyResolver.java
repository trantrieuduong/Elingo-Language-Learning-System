package com.elingo.vocabulary.service;

import com.elingo.common.enums.MediaKind;
import com.elingo.common.event.FileAttachedEvent;
import com.elingo.common.event.FileDeletedEvent;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.file.util.FileKey;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MediaKeyResolver {

    private final ApplicationEventPublisher publisher;

    /**
     * @param key: giá trị lưu DB (key uploads/ hoặc null).
     * @param verifiedKey: có khi gắn file mới.
     * @param obsoleteKeys: file cũ cần xóa.
     * */
    public record Resolved(String key, String verifiedKey, List<String> obsoleteKeys) {
        private static final Resolved EMPTY = new Resolved(null, null, List.of());
    }

    /** (trước save): chỉ kiểm tra, chưa phát event. */
    public Resolved forCreate(String verifiedKey, MediaKind kind) {
        if (isBlank(verifiedKey)) return Resolved.EMPTY;
        if (!FileKey.isInZone(verifiedKey, FileKey.VERIFIED_PREFIX)) {
            throw new AppException(AppError.INVALID_REQUEST);
        }
        return new Resolved(requireKind(verifiedKey, kind), verifiedKey, List.of());
    }

    public Resolved forUpdate(String newKey, String oldKey, MediaKind kind) {
        List<String> obsolete = (!isBlank(oldKey) && FileKey.isInZone(oldKey, FileKey.UPLOADS_PREFIX))
                ? List.of(oldKey) : List.of();

        if (isBlank(newKey)) {                                   // xóa file
            return new Resolved(null, null, obsolete);
        }
        if (FileKey.isInZone(newKey, FileKey.VERIFIED_PREFIX)) { // thay file mới
            return new Resolved(requireKind(newKey, kind), newKey, obsolete);
        }
        if (FileKey.isInZone(newKey, FileKey.UPLOADS_PREFIX) && newKey.equals(oldKey)) {
            return new Resolved(oldKey, null, List.of());        // giữ nguyên
        }
        throw new AppException(AppError.INVALID_REQUEST);        // key lạ / của entity khác
    }

    /** Bước 2 (sau khi ghi entity): phát event. */
    public void publish(Resolved r) {
        if (r.verifiedKey() != null) {
            publisher.publishEvent(new FileAttachedEvent(r.verifiedKey(), r.key()));
        }
        if (!r.obsoleteKeys().isEmpty()) {
            publisher.publishEvent(new FileDeletedEvent(r.obsoleteKeys()));
        }
    }

    private String requireKind(String verifiedKey, MediaKind... allowed) {
        MediaKind actual = FileKey.mediaKindOf(verifiedKey);
        if (actual == null || !List.of(allowed).contains(actual)) {
            throw new AppException(AppError.INVALID_FILE_TYPE);
        }
        return FileKey.toUploads(verifiedKey);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
