package com.elingo.file.service;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.file.dto.request.AbortMultipartRequest;
import com.elingo.file.dto.request.CompleteMultipartRequest;
import com.elingo.file.dto.request.CompletedPartInfo;
import com.elingo.file.dto.request.PresignedUrlRequest;
import com.elingo.file.dto.request.VerifyUploadRequest;
import com.elingo.file.dto.response.CompleteMultipartResponse;
import com.elingo.file.dto.response.InitiateMultipartResponse;
import com.elingo.file.dto.response.PresignedUrlResponse;
import com.elingo.file.dto.response.VerifiedFileResponse;
import com.elingo.file.service.impl.R2ServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.net.URI;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class R2ServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @InjectMocks
    private R2ServiceImpl r2Service;

    private static final Long USER_ID = 12L;
    private static final String BUCKET = "test-bucket";
    private static final String PUBLIC_URL = "https://cdn.test.elingo.com";
    private static final long MIN_PART_SIZE = 5L * 1024 * 1024;

    /** PNG 8 byte đầu chuẩn. */
    private static final byte[] PNG_HEADER = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    /** JPEG 3 byte đầu. */
    private static final byte[] JPEG_HEADER = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(r2Service, "bucket", BUCKET);
        ReflectionTestUtils.setField(r2Service, "publicUrl", PUBLIC_URL);
        ReflectionTestUtils.setField(r2Service, "minPartSize", MIN_PART_SIZE);
        ReflectionTestUtils.setField(r2Service, "presignDurationMinutes", 30L);
    }

    private static URL url(String s) {
        try {
            return URI.create(s).toURL();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static S3Exception s3Error(int statusCode) {
        return (S3Exception) S3Exception.builder().statusCode(statusCode).message("boom").build();
    }

    // ========================================================================
    // afterPropertiesSet — chặn cấu hình sai lúc khởi động
    // ========================================================================
    @Nested
    @DisplayName("afterPropertiesSet validation")
    class ConfigValidation {
        @Test
        @DisplayName("valid config -> no exception")
        void valid() {
            r2Service.afterPropertiesSet();
        }

        @Test
        @DisplayName("minPartSize <= 0 -> IllegalStateException")
        void invalidPartSize() {
            ReflectionTestUtils.setField(r2Service, "minPartSize", 0L);
            assertThatThrownBy(() -> r2Service.afterPropertiesSet())
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("presignDurationMinutes <= 0 -> IllegalStateException")
        void invalidDuration() {
            ReflectionTestUtils.setField(r2Service, "presignDurationMinutes", 0L);
            assertThatThrownBy(() -> r2Service.afterPropertiesSet())
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ========================================================================
    // generatePresignedUrl
    // ========================================================================
    @Nested
    @DisplayName("generatePresignedUrl")
    class GeneratePresignedUrl {
        @Test
        @DisplayName("valid -> key staging/{userId}/ + url has signature")
        void success() {
            PresignedPutObjectRequest presigned = org.mockito.Mockito.mock(PresignedPutObjectRequest.class);
            when(presigned.url()).thenReturn(url("https://signed/put"));
            when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);

            PresignedUrlRequest request = new PresignedUrlRequest("image/png", 1024L);
            PresignedUrlResponse response = r2Service.generatePresignedUrl(request, USER_ID);

            assertThat(response.key()).startsWith("staging/12/").endsWith(".png");
            assertThat(response.url()).isEqualTo("https://signed/put");
            assertThat(response.expiresAt()).isNotNull();
        }

        @Test
        @DisplayName("unsupported file type -> INVALID_FILE_TYPE, no presign")
        void invalidType() {
            PresignedUrlRequest request = new PresignedUrlRequest("application/pdf", 1024L);

            assertThatThrownBy(() -> r2Service.generatePresignedUrl(request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.INVALID_FILE_TYPE));
            verify(s3Presigner, never()).presignPutObject(any(PutObjectPresignRequest.class));
        }

        @Test
        @DisplayName("size exceeds group limit -> FILE_TOO_LARGE")
        void tooLarge() {
            // ảnh trần 5MB
            PresignedUrlRequest request = new PresignedUrlRequest("image/png", 6L * 1024 * 1024);

            assertThatThrownBy(() -> r2Service.generatePresignedUrl(request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.FILE_TOO_LARGE));
        }

        @Test
        @DisplayName("size <= 0 -> INVALID_REQUEST")
        void nonPositiveSize() {
            PresignedUrlRequest request = new PresignedUrlRequest("image/png", 0L);

            assertThatThrownBy(() -> r2Service.generatePresignedUrl(request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.INVALID_REQUEST));
        }
    }

    // ========================================================================
    // initiateMultipart
    // ========================================================================
    @Nested
    @DisplayName("initiateMultipart")
    class InitiateMultipart {
        @Test
        @DisplayName("10MB video with 5MB minimum part size -> 2 parts")
        void success() {
            when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
                    .thenReturn(CreateMultipartUploadResponse.builder().uploadId("upload-1").build());
            PresignedUploadPartRequest presigned = org.mockito.Mockito.mock(PresignedUploadPartRequest.class);
            when(presigned.url()).thenReturn(url("https://signed/part"));
            when(s3Presigner.presignUploadPart(any(UploadPartPresignRequest.class))).thenReturn(presigned);

            PresignedUrlRequest request = new PresignedUrlRequest("video/mp4", 10L * 1024 * 1024);
            InitiateMultipartResponse response = r2Service.initiateMultipart(request, USER_ID);

            assertThat(response.uploadId()).isEqualTo("upload-1");
            assertThat(response.key()).startsWith("staging/12/").endsWith(".mp4");
            assertThat(response.partSize()).isEqualTo(MIN_PART_SIZE);
            assertThat(response.parts()).hasSize(2);
            assertThat(response.parts().get(0).partNumber()).isEqualTo(1);
            assertThat(response.parts().get(1).partNumber()).isEqualTo(2);
            verify(s3Presigner, org.mockito.Mockito.times(2))
                    .presignUploadPart(any(UploadPartPresignRequest.class));
        }

        @Test
        @DisplayName("unsupported file type -> INVALID_FILE_TYPE, no upload")
        void invalidType() {
            PresignedUrlRequest request = new PresignedUrlRequest("application/zip", 1024L);

            assertThatThrownBy(() -> r2Service.initiateMultipart(request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.INVALID_FILE_TYPE));
            verify(s3Client, never()).createMultipartUpload(any(CreateMultipartUploadRequest.class));
        }
    }

    // ========================================================================
    // completeMultipartUpload
    // ========================================================================
    @Nested
    @DisplayName("completeMultipartUpload")
    class CompleteMultipart {
        @Test
        @DisplayName("owned key + complete ok -> returns fileKey")
        void success() {
            CompleteMultipartRequest request = new CompleteMultipartRequest(
                    "staging/12/a.mp4", "upload-1",
                    java.util.List.of(new CompletedPartInfo(1, "etag-1")));

            CompleteMultipartResponse response = r2Service.completeMultipartUpload(request, USER_ID);

            assertThat(response.fileKey()).isEqualTo("staging/12/a.mp4");
            verify(s3Client).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
        }

        @Test
        @DisplayName("other user key -> FILE_ACCESS_DENIED")
        void notOwned() {
            CompleteMultipartRequest request = new CompleteMultipartRequest(
                    "staging/99/a.mp4", "upload-1",
                    java.util.List.of(new CompletedPartInfo(1, "etag-1")));

            assertThatThrownBy(() -> r2Service.completeMultipartUpload(request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.FILE_ACCESS_DENIED));
            verify(s3Client, never()).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
        }

        @Test
        @DisplayName("R2 error on complete -> abort then FILE_UPLOAD_FAILED")
        void completeFails() {
            when(s3Client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
                    .thenThrow(s3Error(500));

            CompleteMultipartRequest request = new CompleteMultipartRequest(
                    "staging/12/a.mp4", "upload-1",
                    java.util.List.of(new CompletedPartInfo(1, "etag-1")));

            assertThatThrownBy(() -> r2Service.completeMultipartUpload(request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.FILE_UPLOAD_FAILED));
            verify(s3Client).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
        }
    }

    // ========================================================================
    // abortMultipart
    // ========================================================================
    @Nested
    @DisplayName("abortMultipart")
    class AbortMultipart {
        @Test
        @DisplayName("owned key -> call abort")
        void success() {
            r2Service.abortMultipart(new AbortMultipartRequest("staging/12/a.mp4", "upload-1"), USER_ID);
            verify(s3Client).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
        }

        @Test
        @DisplayName("other user key -> FILE_ACCESS_DENIED")
        void notOwned() {
            assertThatThrownBy(() -> r2Service.abortMultipart(
                    new AbortMultipartRequest("staging/99/a.mp4", "upload-1"), USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.FILE_ACCESS_DENIED));
            verify(s3Client, never()).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
        }

        @Test
        @DisplayName("R2 error on abort -> swallow error, no throw")
        void abortFailsSwallowed() {
            when(s3Client.abortMultipartUpload(any(AbortMultipartUploadRequest.class)))
                    .thenThrow(s3Error(500));

            // không ném exception
            r2Service.abortMultipart(new AbortMultipartRequest("staging/12/a.mp4", "upload-1"), USER_ID);
        }
    }

    // ========================================================================
    // verifyUploadedFile — chốt chặn chính
    // ========================================================================
    @Nested
    @DisplayName("verifyUploadedFile")
    class VerifyUploadedFile {

        private void stubHead(long size) {
            when(s3Client.headObject(any(HeadObjectRequest.class)))
                    .thenReturn(HeadObjectResponse.builder().contentLength(size).build());
        }

        private void stubHeader(byte[] header) {
            ResponseBytes<GetObjectResponse> bytes =
                    ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), header);
            when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(bytes);
        }

        @Test
        @DisplayName("valid -> copy to verified/, delete staging/, return verifiedFileKey")
        void success() {
            stubHead(1024L);
            stubHeader(PNG_HEADER);

            VerifyUploadRequest request = new VerifyUploadRequest("staging/12/a.png", "image/png");
            VerifiedFileResponse response = r2Service.verifyUploadedFile(request, USER_ID);

            assertThat(response.verifiedFileKey()).startsWith("verified/12/").endsWith(".png");
            assertThat(response.contentType()).isEqualTo("image/png");
            assertThat(response.sizeBytes()).isEqualTo(1024L);
            assertThat(response.verifiedPublicUrl())
                    .isEqualTo(PUBLIC_URL + "/" + response.verifiedFileKey());

            verify(s3Client).copyObject(any(CopyObjectRequest.class));
            // xoá đúng bản staging/
            verify(s3Client).deleteObject(org.mockito.ArgumentMatchers.argThat(
                    (DeleteObjectRequest req) -> req.key().equals("staging/12/a.png")));
        }

        @Test
        @DisplayName("other user key -> FILE_ACCESS_DENIED, no R2 touch")
        void notOwned() {
            VerifyUploadRequest request = new VerifyUploadRequest("staging/99/a.png", "image/png");

            assertThatThrownBy(() -> r2Service.verifyUploadedFile(request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.FILE_ACCESS_DENIED));
            verify(s3Client, never()).headObject(any(HeadObjectRequest.class));
        }

        @Test
        @DisplayName("object does not exist (404) -> FILE_NOT_FOUND")
        void notFound() {
            when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(s3Error(404));

            VerifyUploadRequest request = new VerifyUploadRequest("staging/12/a.png", "image/png");

            assertThatThrownBy(() -> r2Service.verifyUploadedFile(request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.FILE_NOT_FOUND));
        }

        @Test
        @DisplayName("actual size exceeds group limit -> FILE_TOO_LARGE and delete staging/")
        void tooLarge() {
            stubHead(6L * 1024 * 1024); // ảnh trần 5MB

            VerifyUploadRequest request = new VerifyUploadRequest("staging/12/a.png", "image/png");

            assertThatThrownBy(() -> r2Service.verifyUploadedFile(request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.FILE_TOO_LARGE));
            verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
            verify(s3Client, never()).copyObject(any(CopyObjectRequest.class));
        }

        @Test
        @DisplayName("unsupported declared type -> INVALID_FILE_TYPE and delete staging/")
        void invalidDeclaredType() {
            stubHead(1024L);

            VerifyUploadRequest request = new VerifyUploadRequest("staging/12/a.png", "application/pdf");

            assertThatThrownBy(() -> r2Service.verifyUploadedFile(request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.INVALID_FILE_TYPE));
            verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("magic bytes differ from declared type -> FILE_TYPE_MISMATCH and delete staging/")
        void typeMismatch() {
            stubHead(1024L);
            stubHeader(JPEG_HEADER); // thật là jpeg

            VerifyUploadRequest request = new VerifyUploadRequest("staging/12/a.png", "image/png");

            assertThatThrownBy(() -> r2Service.verifyUploadedFile(request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.FILE_TYPE_MISMATCH));
            verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
            verify(s3Client, never()).copyObject(any(CopyObjectRequest.class));
        }

        @Test
        @DisplayName("unrecognized magic bytes -> FILE_TYPE_MISMATCH and delete staging/")
        void unknownSignature() {
            stubHead(1024L);
            stubHeader(new byte[]{0x00, 0x01, 0x02, 0x03});

            VerifyUploadRequest request = new VerifyUploadRequest("staging/12/a.png", "image/png");

            assertThatThrownBy(() -> r2Service.verifyUploadedFile(request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.FILE_TYPE_MISMATCH));
            verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("header read failed (temporary error) -> FILE_UPLOAD_FAILED, do NOT delete staging/")
        void headerReadFails() {
            stubHead(1024L);
            when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(s3Error(500));

            VerifyUploadRequest request = new VerifyUploadRequest("staging/12/a.png", "image/png");

            assertThatThrownBy(() -> r2Service.verifyUploadedFile(request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.FILE_UPLOAD_FAILED));
            // file của user được giữ lại để thử lại
            verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        }
    }

    // ========================================================================
    // deleteFile
    // ========================================================================
    @Nested
    @DisplayName("deleteFile")
    class DeleteFile {
        @Test
        @DisplayName("delete successful -> true")
        void success() {
            assertThat(r2Service.deleteFile("uploads/12/a.png")).isTrue();
            verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("R2 error -> false (swallow error)")
        void failure() {
            when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(s3Error(500));
            assertThat(r2Service.deleteFile("uploads/12/a.png")).isFalse();
        }
    }

    // ========================================================================
    // promoteToUploads — đọc -> copy -> xoá
    // ========================================================================
    @Nested
    @DisplayName("promoteToUploads")
    class PromoteToUploads {

        private static final String VERIFIED_KEY = "verified/12/a.png";
        private static final String UPLOADS_KEY = "uploads/12/a.png";

        @Test
        @DisplayName("normal: verified exists, uploads not -> copy then delete verified/")
        void success() {
            when(s3Client.headObject(any(HeadObjectRequest.class))).thenAnswer(inv -> {
                HeadObjectRequest req = inv.getArgument(0);
                if (req.key().equals(UPLOADS_KEY)) {
                    throw s3Error(404); // uploads chưa có
                }
                return HeadObjectResponse.builder().contentLength(10L).build(); // verified có
            });

            r2Service.promoteToUploads(VERIFIED_KEY, UPLOADS_KEY);

            verify(s3Client).copyObject(any(CopyObjectRequest.class));
            verify(s3Client).deleteObject(org.mockito.ArgumentMatchers.argThat(
                    (DeleteObjectRequest req) -> req.key().equals(VERIFIED_KEY)));
        }

        @Test
        @DisplayName("idempotent: uploads already exists -> no copy, no delete")
        void alreadyPromoted() {
            when(s3Client.headObject(any(HeadObjectRequest.class)))
                    .thenReturn(HeadObjectResponse.builder().contentLength(10L).build());

            r2Service.promoteToUploads(VERIFIED_KEY, UPLOADS_KEY);

            verify(s3Client, never()).copyObject(any(CopyObjectRequest.class));
            verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("key filename mismatch -> IllegalArgumentException, no R2 touch")
        void keyMismatch() {
            assertThatThrownBy(() -> r2Service.promoteToUploads(VERIFIED_KEY, "uploads/12/b.png"))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(s3Client, never()).headObject(any(HeadObjectRequest.class));
        }

        @Test
        @DisplayName("neither verified nor uploads exists -> FILE_NOT_FOUND")
        void bothMissing() {
            when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(s3Error(404));

            assertThatThrownBy(() -> r2Service.promoteToUploads(VERIFIED_KEY, UPLOADS_KEY))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.FILE_NOT_FOUND));
            verify(s3Client, never()).copyObject(any(CopyObjectRequest.class));
        }

        @Test
        @DisplayName("copy failed -> FILE_UPLOAD_FAILED, no delete verified/")
        void copyFails() {
            when(s3Client.headObject(any(HeadObjectRequest.class))).thenAnswer(inv -> {
                HeadObjectRequest req = inv.getArgument(0);
                if (req.key().equals(UPLOADS_KEY)) {
                    throw s3Error(404);
                }
                return HeadObjectResponse.builder().contentLength(10L).build();
            });
            when(s3Client.copyObject(any(CopyObjectRequest.class))).thenThrow(s3Error(500));

            assertThatThrownBy(() -> r2Service.promoteToUploads(VERIFIED_KEY, UPLOADS_KEY))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.FILE_UPLOAD_FAILED));
            verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("copy ok but delete verified/ failed -> still complete, no throw")
        void deleteVerifiedFailsButCompletes() {
            when(s3Client.headObject(any(HeadObjectRequest.class))).thenAnswer(inv -> {
                HeadObjectRequest req = inv.getArgument(0);
                if (req.key().equals(UPLOADS_KEY)) {
                    throw s3Error(404);
                }
                return HeadObjectResponse.builder().contentLength(10L).build();
            });
            when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(s3Error(500));

            // bản verified/ sẽ tự hết hạn; không ném exception
            r2Service.promoteToUploads(VERIFIED_KEY, UPLOADS_KEY);

            verify(s3Client).copyObject(any(CopyObjectRequest.class));
        }
    }
}