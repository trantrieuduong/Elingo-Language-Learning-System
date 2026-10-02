package com.elingo.file.service;

import com.elingo.file.dto.request.CompleteMultipartRequest;
import com.elingo.file.dto.request.PresignedUrlRequest;
import com.elingo.file.dto.request.VerifyUploadRequest;
import com.elingo.file.dto.response.CompleteMultipartResponse;
import com.elingo.file.dto.response.InitiateMultipartResponse;
import com.elingo.file.dto.response.PresignedUrlResponse;
import com.elingo.file.dto.response.VerifiedFileResponse;

public interface R2Service {
    PresignedUrlResponse generatePresignedUrl(PresignedUrlRequest request, Long userId);

    InitiateMultipartResponse initiateMultipart(PresignedUrlRequest request, Long userId);

    CompleteMultipartResponse completeMultipartUpload(CompleteMultipartRequest request, Long userId);

    void abortMultipart(String key, String uploadId, Long userId);

    /**
     * Đọc chữ ký nhị phân của file vừa upload ở {@code stagingKey}, so với Content-Type
     * client đã khai. Hợp lệ thì chuyển sang vùng chờ {@code verified/{userId}/{uuid}.{ext}};
     * sai thì xoá object và báo lỗi.
     *
     * <p>File <b>chưa</b> phải vĩnh viễn ở đây. Nó mới chỉ qua được bước kiểm tra định
     * dạng; chuyển sang {@code uploads/} là việc của {@link #promoteToUploads}, chạy sau
     * khi module sở hữu đã commit bản ghi tham chiếu tới nó. Xem {@link
     * com.elingo.file.util.FileKey} để hiểu vì sao cần ba vùng.
     *
     * <p>File cũ bị thay thế không được xoá ở đây — module sở hữu dữ liệu phát
     * {@link com.elingo.common.event.FileReplacedEvent} sau khi đã commit, và
     * {@code FileCleanupListener} mới xoá.
     */
    VerifiedFileResponse verifyUploadedFile(String stagingKey, VerifyUploadRequest request, Long userId);

    /**
     * Xoá một object trên R2.
     *
     * <p><b>Chỉ dùng nội bộ</b> — gọi từ {@code FileCleanupListener} sau khi event đã
     * commit, với key do module sở hữu dữ liệu lấy từ CSDL của chính nó. Không có endpoint
     * xoá file cho client, và không nên thêm: client không biết file nào còn được dùng, còn
     * key `uploads/{userId}/{uuid}.{ext}` thì lộ ra đủ để dò UUID xoá file của người khác.
     *
     * <p>Vì không có đường vào từ client nên hàm này không kiểm tra quyền sở hữu; phần đó
     * thuộc về các endpoint nhận file ({@code requireOwnedKey}).
     *
     * <p>Lỗi R2 bị nuốt, không ném ra — dọn file là việc phụ, không được làm hỏng thao tác
     * chính. Vì vậy cần biết có xoá thành công không thì phải đọc {@code return}.
     *
     * @return {@code true} nếu xoá thành công; {@code false} nếu R2 báo lỗi (đã ghi log)
     */
    boolean deleteFile(String key);

    /**
     * Chuyển file từ vùng chờ {@code verified/} sang vùng vĩnh viễn {@code uploads/}
     *
     * <p><b>Chỉ dùng nội bộ</b> — gọi từ {@code FilePromotionListener} khi module sở hữu đã
     * commit bản ghi tham chiếu tới file, nên key ở đây do chính module đó lấy từ CSDL của
     * nó. Không có đường vào từ client nên hàm không kiểm tra quyền sở hữu, giống
     * {@link #deleteFile(String)}.
     *
     * <p>Phải chạy <b>sau khi commit</b>: copy trước khi commit thì transaction rollback
     * để lại file mồ côi ở {@code uploads/} — nơi không có quy tắc vòng đời nào dọn.
     *
     * <p>Thứ tự thao tác cố định: đọc trước, copy sau, xoá cuối. Xoá trước rồi copy sau thì
     * hỏng giữa chừng là mất file thật; xoá hỏng sau khi copy thành công thì chỉ còn hai
     * bản, và bản {@code verified/} tự biến mất theo quy tắc vòng đời.
     */
    void promoteToUploads(String verifiedKey, String uploadsKey);
}
