package com.elingo.file.service;

import com.elingo.file.dto.request.AbortMultipartRequest;
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

    void abortMultipart(AbortMultipartRequest request, Long userId);

    /**
     * Đọc chữ ký nhị phân của file vừa upload ở {@code stagingKey}, so với Content-Type
     * client đã khai. Hợp lệ thì chuyển sang vùng chờ {@code verified/{userId}/{uuid}.{ext}};
     * sai thì xoá object và báo lỗi.
     */
    VerifiedFileResponse verifyUploadedFile(VerifyUploadRequest request, Long userId);

    /**
     * Xoá một object trên R2.
     *
     * <p><b>Chỉ dùng nội bộ</b>
     *
     * @return {@code true} nếu xoá thành công; {@code false} nếu R2 báo lỗi (đã ghi log)
     */
    boolean deleteFile(String key);

    /**
     * Chuyển file từ vùng chờ {@code verified/} sang vùng vĩnh viễn {@code uploads/}
     *
     * <p><b>Chỉ dùng nội bộ</b>
     */
    void promoteToUploads(String verifiedKey, String uploadsKey);
}
