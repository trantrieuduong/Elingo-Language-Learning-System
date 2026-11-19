package com.elingo.file.util;

import com.elingo.common.enums.MediaKind;
import com.elingo.common.enums.SystemFileModule;
import com.elingo.common.event.FileAttachedEvent;
import com.elingo.common.event.FileDeletedEvent;
import com.elingo.common.event.SystemFileAttachedEvent;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MediaKeyResolver {

    private final ApplicationEventPublisher publisher;

    /**
     * @param key: giá trị lưu DB (key uploads/, system/ hoặc null).
     * @param verifiedKey: có khi gắn file mới.
     * @param obsoleteKeys: file cũ cần xóa.
     * */
    public record Resolved(String key, String verifiedKey, List<String> obsoleteKeys) {
        private static final Resolved EMPTY = new Resolved(null, null, List.of());
    }

    /** (trước save): chỉ kiểm tra, chưa phát event. Dành cho file người dùng (uploads/). */
    public Resolved forCreate(String verifiedKey, MediaKind kind) {
        if (isBlank(verifiedKey)) return Resolved.EMPTY;
        if (!FileKey.isInZone(verifiedKey, FileKey.VERIFIED_PREFIX)) {
            throw new AppException(AppError.INVALID_REQUEST);
        }
        return new Resolved(requireKind(verifiedKey, kind), verifiedKey, List.of());
    }

    /** (trước save): Dành cho file hệ thống của Admin (system/{module}/). */
    public Resolved forCreate(String verifiedKey, SystemFileModule module) {
        if (isBlank(verifiedKey)) return Resolved.EMPTY;
        if (!FileKey.isInZone(verifiedKey, FileKey.VERIFIED_PREFIX)) {
            throw new AppException(AppError.INVALID_REQUEST);
        }
        return new Resolved(requireModuleKind(verifiedKey, module), verifiedKey, List.of());
    }

    /** Cập nhật file cho người dùng (uploads/). */
    public Resolved forUpdate(String newKey, String oldKey, MediaKind kind) {
        List<String> obsolete = (!isBlank(oldKey) && FileKey.isPermanentKey(oldKey))
                ? List.of(oldKey) : List.of();

        if (isBlank(newKey)) {                                   // xóa file
            return new Resolved(null, null, obsolete);
        }
        if (FileKey.isInZone(newKey, FileKey.VERIFIED_PREFIX)) { // thay file mới
            return new Resolved(requireKind(newKey, kind), newKey, obsolete);
        }
        if (FileKey.isPermanentKey(newKey) && newKey.equals(oldKey)) {
            return new Resolved(oldKey, null, List.of());        // giữ nguyên (cả uploads/ lẫn system/)
        }
        throw new AppException(AppError.INVALID_REQUEST);        // key lạ / của entity khác
    }

    /** Cập nhật file cho hệ thống của Admin (system/{module}/). */
    public Resolved forUpdate(String newKey, String oldKey, SystemFileModule module) {
        List<String> obsolete = (!isBlank(oldKey) && FileKey.isPermanentKey(oldKey))
                ? List.of(oldKey) : List.of();

        if (isBlank(newKey)) {                                   // xóa file
            return new Resolved(null, null, obsolete);
        }
        if (FileKey.isInZone(newKey, FileKey.VERIFIED_PREFIX)) { // thay file mới
            return new Resolved(requireModuleKind(newKey, module), newKey, obsolete);
        }
        if (FileKey.isPermanentKey(newKey) && newKey.equals(oldKey)) {
            return new Resolved(oldKey, null, List.of());        // giữ nguyên (cả uploads/ lẫn system/)
        }
        throw new AppException(AppError.INVALID_REQUEST);        // key lạ / của entity khác
    }

    /** Bước 2 (sau khi ghi entity): phát event. */
    public void publish(Resolved r) {
        if (r.verifiedKey() != null) {
            if (FileKey.isInZone(r.key(), FileKey.SYSTEM_PREFIX)) {
                publisher.publishEvent(new SystemFileAttachedEvent(r.verifiedKey(), r.key()));
            } else {
                publisher.publishEvent(new FileAttachedEvent(r.verifiedKey(), r.key()));
            }
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

    private String requireModuleKind(String verifiedKey, SystemFileModule module) {
        if (module == null) {
            throw new AppException(AppError.INVALID_REQUEST);
        }
        MediaKind actual = FileKey.mediaKindOf(verifiedKey);
        if (actual == null || !module.supports(actual)) {
            throw new AppException(AppError.INVALID_FILE_TYPE);
        }
        return FileKey.toSystem(verifiedKey, module);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
