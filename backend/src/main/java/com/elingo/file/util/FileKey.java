package com.elingo.file.util;

/**
 * <p>Bucket chia làm ba vùng, mỗi vùng một ý nghĩa khác nhau:
 *
 * <pre>
 *   staging/{userId}/{uuid}.{ext}     file vừa tải lên, chưa kiểm tra định dạng
 *   verified/{userId}/{uuid}.{ext}    đã qua kiểm tra magic bytes, chưa ai dùng
 *   uploads/{userId}/{uuid}.{ext}     đã được một bản ghi tham chiếu
 * </pre>
 *
 * <p><b>Vì sao có ba vùng:</b> {@code staging/} và {@code verified/} đều được quy tắc
 * vòng đời của R2 xoá sau 7 ngày, nên file người dùng tải lên rồi không dùng tới sẽ tự
 * biến mất mà không cần ai theo dõi. Chỉ khi module sở hữu dữ liệu ghi key vào CSDL và
 * commit xong, file mới rời {@code verified/} sang {@code uploads/} — nơi không có quy
 * tắc nào đụng tới. Nhờ vậy {@code uploads/} không bao giờ chứa file nào mà CSDL chưa
 * tham chiếu, mà không cần một bảng CSDL nào để theo dõi.
 *
 * <p><b>Tên file không đổi khi đổi vùng.</b> {@code verified/12/a.png} và
 * {@code uploads/12/a.png} là cùng một file ở hai nơi, nên việc sửa chữa hậu kỳ chỉ cần
 * biết tên file là dò lại được.
 */
public final class FileKey {

    public static final String STAGING_PREFIX = "staging";

    public static final String VERIFIED_PREFIX = "verified";

    public static final String UPLOADS_PREFIX = "uploads";

    private FileKey() {
    }

    /**
     * Key của bản đã kiểm tra, cùng tên với bản trong {@code staging/}.
     *
     * @throws IllegalArgumentException nếu {@code stagingKey} không thuộc vùng staging
     */
    public static String toVerified(String stagingKey) {
        return replacePrefix(stagingKey, STAGING_PREFIX, VERIFIED_PREFIX);
    }

    /**
     * Key vĩnh viễn, cùng tên với bản trong {@code verified/}.
     *
     * <p>Module sở hữu dữ liệu gọi hàm này để biết trước đường dẫn sẽ ghi vào CSDL, rồi
     * truyền cả hai key lên {@code FileAttachedEvent} — việc đổi vùng nằm ở đây để
     * module khác không phải biết mặt khuất của module {@code file}.
     *
     * @throws IllegalArgumentException nếu {@code verifiedKey} không thuộc vùng verified
     */
    public static String toUploads(String verifiedKey) {
        return replacePrefix(verifiedKey, VERIFIED_PREFIX, UPLOADS_PREFIX);
    }

    /**
     * Đổi vùng nhưng giữ nguyên phần còn lại của key.
     *
     * <p>Phải so khớp cả dấu gạch chéo: {@code "uploads2/12/a.png"} không phải key trong
     * vùng {@code uploads}. Ném thay vì trả về key sai — key sai sẽ lọt vào CSDL và tệ hơn
     * là lỗi ngay tại chỗ này.
     */
    private static String replacePrefix(String key, String fromPrefix, String toPrefix) {
        String required = fromPrefix + "/";
        if (key == null || !key.startsWith(required)) {
            throw new IllegalArgumentException(
                    "Key must start with '" + required + "' but was: " + key);
        }
        return toPrefix + key.substring(fromPrefix.length());
    }
}
