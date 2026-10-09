package com.elingo.file.util;

import com.elingo.common.enums.MediaKind;

/**
 * <p>Bucket chia làm ba vùng, mỗi vùng một ý nghĩa khác nhau:
 *
 * <pre>
 *   staging/{userId}/{uuid}.{ext}     file vừa tải lên, chưa kiểm tra định dạng
 *   verified/{userId}/{uuid}.{ext}    đã qua kiểm tra magic bytes, chưa ai dùng
 *   uploads/{userId}/{uuid}.{ext}     đã được một bản ghi tham chiếu
 * </pre>
 *
 * <p><b>Tên file không đổi khi promote từ verified sang uploads.</b>
 * {@code verified/12/a.png} và {@code uploads/12/a.png} là cùng một file ở hai nơi, nên
 * việc sửa chữa hậu kỳ chỉ cần biết tên file là dò lại được. (Bước staging -> verified thì
 * sinh tên mới, vì lúc đó mới biết đuôi file thật.)
 */
public final class FileKey {

    public static final String STAGING_PREFIX = "staging";

    public static final String VERIFIED_PREFIX = "verified";

    public static final String UPLOADS_PREFIX = "uploads";

    private FileKey() {
    }

    /**
     * Key vĩnh viễn, cùng tên với bản trong {@code verified/}.
     *
     * <p>Module sở hữu dữ liệu gọi hàm này để biết trước đường dẫn sẽ ghi vào CSDL, rồi
     * truyền cả hai key lên {@code FileAttachedEvent}
     *
     * @throws IllegalArgumentException nếu {@code verifiedKey} không thuộc vùng verified
     */
    public static String toUploads(String verifiedKey) {
        if (!isInZone(verifiedKey, VERIFIED_PREFIX)) {
            throw new IllegalArgumentException(
                    "Key must start with '" + VERIFIED_PREFIX + "/' but was: " + verifiedKey);
        }

        return UPLOADS_PREFIX + verifiedKey.substring(VERIFIED_PREFIX.length());
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
        String base = publicUrl.endsWith("/")
                ? publicUrl.substring(0, publicUrl.length() - 1)
                : publicUrl;
        return base + "/" + fileKey;
    }
}