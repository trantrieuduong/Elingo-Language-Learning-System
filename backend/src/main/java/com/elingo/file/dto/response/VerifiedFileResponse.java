package com.elingo.file.dto.response;

/**
 * Kết quả kiểm tra một file vừa tải lên.
 *
 * <p><b>Hai trường đầu trỏ vào vùng chờ, không phải bản vĩnh viễn.</b> File còn nằm ở
 * {@code verified/} — vùng có quy tắc vòng đời xoá sau 7 ngày — cho tới khi module sở
 * hữu dữ liệu ghi key vào CSDL và commit. Nếu người dùng tải lên rồi không dùng tới, file
 * tự biến mất, không cần ai dọn.
 *
 * <p>Client dán {@code verifiedFileKey} vào request cập nhật bản ghi; module sở hữu tự
 * tính key {@code uploads/} rồi lưu vào CSDL. URL vĩnh viễn lấy từ response của bản ghi sau
 * khi cập nhật, không lấy từ đây.
 *
 * @param verifiedFileKey  key ở vùng chờ {@code verified/}
 * @param verifiedPublicUrl URL xem trước, dùng được tối đa 7 ngày
 * @param contentType      mime type suy ra từ magic bytes, không phải cái client khai
 * @param sizeBytes        kích thước thật đọc từ R2
 */
public record VerifiedFileResponse(
        String verifiedFileKey,
        String verifiedPublicUrl,
        String contentType,
        Long sizeBytes
) {
}
