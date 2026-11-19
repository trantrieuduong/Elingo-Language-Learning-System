package com.elingo.file.util;

import com.elingo.common.enums.MediaKind;
import com.elingo.common.enums.SystemFileModule;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;

/**
 * <p>Bucket chia làm các vùng lưu trữ:
 *
 * <pre>
 *   staging/{userId}/{uuid}.{ext}        file của user vừa tải lên, chưa kiểm tra định dạng
 *   verified/{userId}/{uuid}.{ext}       file của user đã qua kiểm tra magic bytes, chưa dùng
 *   uploads/{userId}/{uuid}.{ext}        file của user đã được một bản ghi tham chiếu
 *   system/{module}/{uuid}.{ext}         file hệ thống lưu trữ vĩnh viễn (ví dụ: system/decks, system/cards, ...)
 * </pre>
 *
 * <p><b>Tên file không đổi khi promote từ verified sang vùng vĩnh viễn.</b>
 */
public final class FileKey {

    public static final String STAGING_PREFIX = "staging";

    public static final String VERIFIED_PREFIX = "verified";

    public static final String UPLOADS_PREFIX = "uploads";

    public static final String SYSTEM_PREFIX = "system";

    private FileKey() {
    }

    /**
     * Key vĩnh viễn, cùng tên với bản trong {@code verified/}.
     *
     * <p>Module sở hữu dữ liệu gọi hàm này để biết trước đường dẫn sẽ ghi vào CSDL, rồi
     * truyền cả hai key lên {@code FileAttachedEvent}
     *
     * @throws AppException nếu {@code verifiedKey} không thuộc vùng verified
     */
    public static String toUploads(String verifiedKey) {
        if (!isInZone(verifiedKey, VERIFIED_PREFIX)) {
            throw new AppException(AppError.INVALID_REQUEST);
        }

        return UPLOADS_PREFIX + verifiedKey.substring(VERIFIED_PREFIX.length());
    }

    /**
     * Chuyển đổi key ở vùng verified sang key lưu trữ vĩnh viễn của hệ thống:
     * {@code verified/{userId}/{uuid}.{ext}} -&gt; {@code system/{module}/{uuid}.{ext}}.
     *
     * <p><b>Dành riêng cho các thành phần quản trị hệ thống (Admin):</b>
     * Khi Admin tải file lên qua luồng kiểm thực thông thường của người dùng, service xử lý
     * nội dung hệ thống gọi hàm này để sinh đường dẫn lưu trữ vĩnh viễn dạng {@code system/{module}/{uuid}.{ext}}
     * trước khi lưu vào CSDL và kích hoạt sự kiện đồng bộ lưu trữ trên R2.
     *
     * @param verifiedKey key trong vùng verified (dạng {@code verified/{userId}/{uuid}.{ext}})
     * @param module      module hệ thống đích sở hữu file (ví dụ: {@link SystemFileModule#DECKS}, {@link SystemFileModule#CARDS}, ...)
     * @return key lưu trữ vĩnh viễn của hệ thống (dạng {@code system/{module}/{uuid}.{ext}})
     * @throws AppException nếu key không thuộc vùng verified, module null hoặc key sai định dạng
     */
    public static String toSystem(String verifiedKey, SystemFileModule module) {
        if (!isInZone(verifiedKey, VERIFIED_PREFIX) || module == null) {
            throw new AppException(AppError.INVALID_REQUEST);
        }

        int lastSlash = verifiedKey.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == verifiedKey.length() - 1) {
            throw new AppException(AppError.INVALID_REQUEST);
        }
        String fileName = verifiedKey.substring(lastSlash + 1);
        String targetModule = module.getPath();

        return SYSTEM_PREFIX + "/" + targetModule + "/" + fileName;
    }

    /**
     * Kiểm tra key có nằm trong một trong các vùng lưu trữ vĩnh viễn (uploads/ hoặc system/) hay không.
     */
    public static boolean isPermanentKey(String fileKey) {
        return isInZone(fileKey, UPLOADS_PREFIX) || isInZone(fileKey, SYSTEM_PREFIX);
    }

    /**
     * Nhóm định dạng của file mà key này trỏ tới.
     *
     * <p>Module sở hữu dữ liệu gọi hàm này để chặn file sai chỗ <b>trong transaction,
     * trước {@code save()}</b>: avatar chỉ nhận {@link MediaKind#IMAGE}, audio chỉ nhận
     * {@link MediaKind#AUDIO}.
     *
     * @return nhóm định dạng, hoặc {@code null} nếu key không có đuôi hợp lệ
     */
    public static MediaKind mediaKindOf(String fileKey) {
        if (fileKey == null) {
            return null;
        }
        int lastSlash = fileKey.lastIndexOf('/');
        String fileName = lastSlash >= 0 ? fileKey.substring(lastSlash + 1) : fileKey;
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return null;
        }
        return MediaKind.fromExtension(fileName.substring(dot + 1));
    }

    /**
     * Key có nằm trong đúng vùng không.
     *
     * <p>Dùng để chặn một cặp key lệch vùng trước khi đụng vào bucket — xem
     * {@link com.elingo.file.service.R2Service#promoteToUploads}.
     */
    public static boolean isInZone(String fileKey, String zone) {
        return fileKey != null && fileKey.startsWith(zone + "/");
    }

    /**
     * Key có nằm trong thư mục của user trong vùng đó không
     * {@code {zone}/{userId}/}.
     *
     * <p>Đây là toàn bộ cơ chế chống IDOR của module: userId lấy từ token, không cần tra
     * CSDL.
     */
    public static boolean isOwnedBy(String fileKey, Long userId) {
        if (userId == null || fileKey == null || fileKey.contains("..")) {
            return false;
        }
        String[] parts = fileKey.split("/", 3);
        return parts.length == 3
                && !parts[0].isEmpty()
                && parts[1].equals(String.valueOf(userId));
    }

    /**
     * URL công khai của một object, ghép từ CDN domain và key.
     *
     * @param publicUrl domain CDN, có hoặc không có dấu gạch chéo cuối
     * @param fileKey   key đầy đủ, ví dụ {@code uploads/12/a.png}
     */
    public static String toPublicUrl(String publicUrl, String fileKey) {
        if (fileKey == null) return null;

        String normalizedKey = fileKey.replaceAll("^/+", "");
        String base = publicUrl.endsWith("/")
                ? publicUrl.substring(0, publicUrl.length() - 1)
                : publicUrl;
        return base + "/" + normalizedKey;
    }

    /**
     * Key của một object
     */
    public static String toKey(String publicUrl, String value) {
        if (value == null || value.isBlank()) return null;

        String v = value.trim();
        String base = publicUrl.endsWith("/")
                ? publicUrl.substring(0, publicUrl.length() - 1)
                : publicUrl;

        if (v.startsWith(base + "/")) {
            v = v.substring(base.length() + 1);
        } else if (v.equals(base) || v.startsWith(base + "?") || v.startsWith(base + "#")) {
            v = "";
        } else if (v.startsWith("http://") || v.startsWith("https://")) {
            // URL của domain khác thì không chấp nhận
            throw new AppException(AppError.INVALID_REQUEST);
        }

        v = v.replaceAll("[?#].*$", "").replaceAll("^/+", "");

        return v.isEmpty() ? null : v;
    }
}
