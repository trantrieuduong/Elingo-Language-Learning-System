package com.elingo.file;

import com.elingo.BaseIntegrationTest;
import com.elingo.auth.service.JwtService;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FileIntegrationTest extends BaseIntegrationTest {

    private static final String PASSWORD = "Password123@";
    private static final String PUBLIC_URL = "https://cdn.test.elingo.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private S3Client s3Client;

    private User owner;
    private User stranger;
    private String ownerToken;
    private String strangerToken;

    @BeforeEach
    void setUp() {
        owner = createUser("fileowner", "fileowner@gmail.com");
        stranger = createUser("filestranger", "filestranger@gmail.com");
        ownerToken = tokenFor(owner);
        strangerToken = tokenFor(stranger);
    }

    @Nested
    @DisplayName("Verify uploaded file")
    class VerifyUploadedFile {

        @Test
        @DisplayName("Valid: the file lands in the waiting zone, key is verified/{id}/{uuid}.{ext}")
        void testVerifyUploadedFile_MovesToWaitingZone() throws Exception {
            String stagingKey = stagingKeyOf(owner, "avatar.png");
            mockStoredFile(pngBytes(), 2048L);

            mockMvc.perform(post("/files/uploads")
                            .header("Authorization", "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new VerifyPayload(stagingKey, "image/png"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.contentType").value("image/png"))
                    .andExpect(jsonPath("$.data.sizeBytes").value(2048))
                    // Tên là UUID nên không so khớp chính xác được, chỉ kiểm đúng hình dạng key.
                    .andExpect(jsonPath("$.data.verifiedFileKey")
                            .value(org.hamcrest.Matchers.matchesRegex(
                                    "verified/" + owner.getId() + "/[0-9a-f-]{36}\\.png")))
                    .andExpect(jsonPath("$.data.verifiedPublicUrl")
                            .value(org.hamcrest.Matchers.startsWith(
                                    PUBLIC_URL + "/verified/" + owner.getId() + "/")));

            ArgumentCaptor<CopyObjectRequest> copyCaptor = ArgumentCaptor.forClass(CopyObjectRequest.class);
            Mockito.verify(s3Client).copyObject(copyCaptor.capture());
            assertThat(copyCaptor.getValue().copySource()).endsWith("/" + stagingKey);
            assertThat(copyCaptor.getValue().destinationKey())
                    .startsWith("verified/" + owner.getId() + "/")
                    .endsWith(".png")
                    .isNotEqualTo(stagingKey);
        }

        @Test
        @DisplayName("Verifying never writes to uploads/ — that zone is only reached after commit")
        void testVerifyUploadedFile_NeverTouchesUploadsZone() throws Exception {
            String stagingKey = stagingKeyOf(owner, "avatar.png");
            mockStoredFile(pngBytes(), 2048L);

            mockMvc.perform(post("/files/uploads")
                            .header("Authorization", "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new VerifyPayload(stagingKey, "image/png"))))
                    .andExpect(status().isOk());

            ArgumentCaptor<CopyObjectRequest> copyCaptor = ArgumentCaptor.forClass(CopyObjectRequest.class);
            Mockito.verify(s3Client).copyObject(copyCaptor.capture());

            // Vào uploads/ trước khi commit nghĩa là rollback sẽ để lại file mồ côi ở nơi
            // không có quy tắc vòng đời nào dọn.
            assertThat(copyCaptor.getValue().destinationKey()).doesNotStartWith("uploads/");
        }

        @Test
        @DisplayName("The staging copy is deleted so no duplicate survives in staging/")
        void testVerifyUploadedFile_DeletesStagingCopy() throws Exception {
            String stagingKey = stagingKeyOf(owner, "avatar.png");
            mockStoredFile(pngBytes(), 2048L);

            mockMvc.perform(post("/files/uploads")
                            .header("Authorization", "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new VerifyPayload(stagingKey, "image/png"))))
                    .andExpect(status().isOk());

            ArgumentCaptor<DeleteObjectRequest> deleteCaptor =
                    ArgumentCaptor.forClass(DeleteObjectRequest.class);
            Mockito.verify(s3Client).deleteObject(deleteCaptor.capture());
            assertThat(deleteCaptor.getValue().key()).isEqualTo(stagingKey);
        }

        @Test
        @DisplayName("Extension taken from real mime, not the name sent by client")
        void testVerifyUploadedFile_UsesRealMimeForExtension() throws Exception {
            // Tên staging là .png nhưng nội dung là JPEG — đuôi phải ra .jpg.
            String stagingKey = stagingKeyOf(owner, "lies.png");
            mockStoredFile(jpegBytes(), 3000L);

            mockMvc.perform(post("/files/uploads")
                            .header("Authorization", "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new VerifyPayload(stagingKey, "image/jpeg"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.verifiedFileKey")
                            .value(org.hamcrest.Matchers.endsWith(".jpg")));

            ArgumentCaptor<CopyObjectRequest> copyCaptor = ArgumentCaptor.forClass(CopyObjectRequest.class);
            Mockito.verify(s3Client).copyObject(copyCaptor.capture());
            assertThat(copyCaptor.getValue().destinationKey())
                    .startsWith("verified/" + owner.getId() + "/")
                    .endsWith(".jpg");
        }

        @Test
        @DisplayName("File renamed .exe declared as image/png is rejected and deleted from R2")
        void testVerifyUploadedFile_RejectsExecutableDisguisedAsImage() throws Exception {
            String stagingKey = stagingKeyOf(owner, "virus.png");
            mockStoredFile(executableBytes(), 4096L);

            mockMvc.perform(post("/files/uploads")
                            .header("Authorization", "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new VerifyPayload(stagingKey, "image/png"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("FILE_TYPE_MISMATCH"));

            // Object sai định dạng phải bị xoá, không được để lại trên bucket.
            Mockito.verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
            Mockito.verify(s3Client, Mockito.never()).copyObject(any(CopyObjectRequest.class));
        }

        @Test
        @DisplayName("Actual content different from declared Content-Type is also rejected")
        void testVerifyUploadedFile_RejectsMismatchedDeclaration() throws Exception {
            String stagingKey = stagingKeyOf(owner, "photo.png");
            // Client khai image/png nhưng gửi byte JPEG.
            mockStoredFile(jpegBytes(), 512L);

            mockMvc.perform(post("/files/uploads")
                            .header("Authorization", "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new VerifyPayload(stagingKey, "image/png"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("FILE_TYPE_MISMATCH"));
        }

        @Test
        @DisplayName("IDOR: another user cannot verify a file not owned by them")
        void testVerifyUploadedFile_RejectsNonOwner() throws Exception {
            String stagingKey = stagingKeyOf(owner, "private.png");
            mockStoredFile(pngBytes(), 1024L);

            mockMvc.perform(post("/files/uploads")
                            .header("Authorization", "Bearer " + strangerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new VerifyPayload(stagingKey, "image/png"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errors[0].code").value("FILE_ACCESS_DENIED"));

            // Không được đụng tới R2 khi kiểm tra quyền đã thất bại.
            Mockito.verify(s3Client, Mockito.never()).headObject(any(HeadObjectRequest.class));
            Mockito.verify(s3Client, Mockito.never()).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("Non-existent key on R2 returns 404")
        void testVerifyUploadedFile_ReturnsNotFound() throws Exception {
            givenMissingObject(stagingKeyOf(owner, "ghost.png"));

            mockMvc.perform(post("/files/uploads")
                            .header("Authorization", "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new VerifyPayload("staging/" + owner.getId() + "/ghost.png",
                                            "image/png"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errors[0].code").value("FILE_NOT_FOUND"));
        }
    }

    private String stagingKeyOf(User user, String fileName) {
        return "staging/" + user.getId() + "/" + fileName;
    }

    private void givenMissingObject(String key) {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.builder()
                .message("Not Found")
                .build());
    }

    /** headObject trả size thật; getObjectAsBytes trả 64 byte đầu dùng để dò magic bytes. */
    private void mockStoredFile(byte[] header, long realSize) {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(
                HeadObjectResponse.builder()
                        .contentLength(realSize)
                        .contentType("application/octet-stream")
                        .build());
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(
                ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), header));
    }

    private User createUser(String username, String email) {
        return userRepository.save(User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .fullName("Test User " + username)
                .isActive(true)
                .isVerified(true)
                .build());
    }

    private String tokenFor(User user) {
        String authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        return jwtService.generateAccessToken(user.getId().toString(), authorities);
    }

    private static byte[] pad(byte[] signature) {
        byte[] padded = new byte[64];
        System.arraycopy(signature, 0, padded, 0, signature.length);
        return padded;
    }

    private static byte[] pngBytes() {
        return pad(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
    }

    private static byte[] jpegBytes() {
        return pad(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00});
    }

    private static byte[] executableBytes() {
        return pad("MZ".getBytes(StandardCharsets.US_ASCII));
    }

    private record VerifyPayload(String fileKey, String fileType) {
    }
}
