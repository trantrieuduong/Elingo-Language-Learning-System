package com.elingo.user;

import com.elingo.BaseIntegrationTest;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.service.EmailService;
import com.elingo.common.service.OtpService;
import com.elingo.common.service.RateLimiterService;
import com.elingo.common.util.EmailTemplateName;
import com.elingo.common.util.OtpType;
import com.elingo.user.dto.request.SendOTPUpdateEmailRequest;
import com.elingo.user.dto.request.UpdateEmailRequest;
import com.elingo.user.dto.request.UpdateProfileRequest;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.elingo.auth.service.JwtService;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserIntegrationTest extends BaseIntegrationTest {
        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserRepository userRepository;

        @MockitoBean
        private EmailService emailService;// Mock để tránh gửi mail rác ra ngoài thật

        @MockitoBean
        private OtpService otpService;

        @MockitoBean
        private RateLimiterService rateLimiterService;

        @Autowired
        private JwtService jwtService;

        @Autowired
        private PasswordEncoder passwordEncoder;

        private User testUser;
        private String authToken;
        private final String PASSWORD = "Password123@";
        private final String OTP = "123456";
        private final String OLD_EMAIL = "old@gmail.com";
        private final String NEW_EMAIL = "new@gmail.com";

        @BeforeEach
        void setUp() {
                testUser = User.builder()
                                .username("testuser")
                                .email(OLD_EMAIL)
                                .passwordHash(passwordEncoder.encode(PASSWORD))
                                .fullName("Test User")
                                .isActive(true)
                                .isVerified(true)
                                .build();
                userRepository.save(testUser);

                // Sinh JWT thật để requests pass security filter chain
                String authorities = testUser.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(Collectors.joining(","));
                authToken = jwtService.generateAccessToken(testUser.getId().toString(), authorities);
        }

        @Test
        @DisplayName("Send OTP update email: Success")
        void testSendOTPUpdateEmail_Success() throws Exception {
                SendOTPUpdateEmailRequest request = new SendOTPUpdateEmailRequest(NEW_EMAIL);
                mockMvc.perform(post("/users/me/email/otp")
                                .header("Authorization", "Bearer " + authToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));
                // $ : Đại diện cho phần tử gốc (root object) của chuỗi JSON

                // Kiểm tra emailService
                Mockito.verify(emailService).sendEmail(
                                eq(NEW_EMAIL), // eq: Kiểm tra tham số thứ nhất có giá trị bằng chính xác với NEW_EMAIL
                                eq(testUser.getUsername()),
                                eq(EmailTemplateName.SEND_OTP),
                                Mockito.any(), // OTP được mock
                                anyString());
        }

        @Test
        @DisplayName("Send OTP update email: Fail because of unchanged email")
        void testSendOTPUpdateEmail_FailUnchangedEmail() throws Exception {
                SendOTPUpdateEmailRequest request = new SendOTPUpdateEmailRequest(OLD_EMAIL);
                mockMvc.perform(post("/users/me/email/otp")
                                .header("Authorization", "Bearer " + authToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.errors[0].code").value("EMAIL_UNCHANGED"));
        }

        void generateAnExistingUser() {
                User existingUser = User.builder()
                                .username("otheruser")
                                .email(NEW_EMAIL)
                                .passwordHash(passwordEncoder.encode(PASSWORD))
                                .fullName("Other User")
                                .isActive(true)
                                .isVerified(true)
                                .build();
                userRepository.save(existingUser);
        }

        @Test
        @DisplayName("Send OTP update email: Fail because of existed email")
        void testSendOTPUpdateEmail_FailExistedEmail() throws Exception {
                generateAnExistingUser();

                SendOTPUpdateEmailRequest request = new SendOTPUpdateEmailRequest(NEW_EMAIL);
                mockMvc.perform(post("/users/me/email/otp")
                                .header("Authorization", "Bearer " + authToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.errors[0].code").value("EMAIL_EXISTED"));
        }

        @Test
        @DisplayName("Update email: Success")
        void testUpdateEmail_Success() throws Exception {
                Mockito.when(otpService.generateAndSaveOtp(OtpType.CHANGE_EMAIL, NEW_EMAIL)).thenReturn(OTP);
                Mockito.doNothing().when(otpService).verifyOtp(OtpType.CHANGE_EMAIL, NEW_EMAIL, OTP);

                UpdateEmailRequest request = new UpdateEmailRequest(NEW_EMAIL, OTP, PASSWORD);
                mockMvc.perform(patch("/users/me/email")
                                .header("Authorization", "Bearer " + authToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
                assert updatedUser.getEmail().equals(NEW_EMAIL);
        }

        @Test
        @DisplayName("Update email: Fail because of existed email")
        void testUpdateEmail_Fail_ExistedEmail() throws Exception {
                generateAnExistingUser();

                UpdateEmailRequest request = new UpdateEmailRequest(NEW_EMAIL, OTP, PASSWORD);
                mockMvc.perform(patch("/users/me/email")
                                .header("Authorization", "Bearer " + authToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.errors[0].code").value("EMAIL_EXISTED"));
        }

        @Test
        @DisplayName("Update Email: Fail because of wrong password")
        void testUpdateEmail_Fail_WrongPassword() throws Exception {
                UpdateEmailRequest request = new UpdateEmailRequest(NEW_EMAIL, OTP, "WrongPassword123@");
                mockMvc.perform(patch("/users/me/email")
                                .header("Authorization", "Bearer " + authToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.errors[0].code").value("PASSWORD_INCORRECT"));
        }

        @Test
        @DisplayName("Update email: Fail because of wrong OTP")
        void testUpdateEmail_Fail_WrongOTP() throws Exception {
                String WRONG_OTP = "000000";
                Mockito.doThrow(new AppException(AppError.OTP_INVALID))
                                .when(otpService).verifyOtp(OtpType.CHANGE_EMAIL, NEW_EMAIL, WRONG_OTP);

                UpdateEmailRequest request = new UpdateEmailRequest(NEW_EMAIL, WRONG_OTP, PASSWORD);
                mockMvc.perform(patch("/users/me/email")
                                .header("Authorization", "Bearer " + authToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.errors[0].code").value("OTP_INVALID"));
        }

        @Test
        @DisplayName("Update profile: Success — assert response and DB persist")
        void testUpdateProfile_Success() throws Exception {
                UpdateProfileRequest request = new UpdateProfileRequest(null, "New Full Name", "newusername");
                mockMvc.perform(put("/users/me")
                                .header("Authorization", "Bearer " + authToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.fullName").value("New Full Name"))
                                .andExpect(jsonPath("$.data.username").value("newusername"));

                User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
                assertThat(updatedUser.getFullName()).isEqualTo("New Full Name");
                assertThat(updatedUser.getUsername()).isEqualTo("newusername");
                assertThat(updatedUser.getUsernameChangedAt()).isNotNull();
        }

        @Test
        @DisplayName("Update profile: Fail — unauthenticated (no JWT) → 403")
        void testUpdateProfile_Fail_Unauthenticated() throws Exception {
                UpdateProfileRequest request = new UpdateProfileRequest(null, "New Name", "testuser");
                mockMvc.perform(put("/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Update profile: Fail — invalid username (contains space)")
        void testUpdateProfile_Fail_InvalidUsername() throws Exception {
                UpdateProfileRequest request = new UpdateProfileRequest(null, "Test User", "a b");
                mockMvc.perform(put("/users/me")
                                .header("Authorization", "Bearer " + authToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.errors[0].code").value("USERNAME_INVALID"));
        }

        @Test
        @DisplayName("Update profile: Fail — fullName too long (151 chars)")
        void testUpdateProfile_Fail_FullNameTooLong() throws Exception {
                String tooLong = "A".repeat(151);
                UpdateProfileRequest request = new UpdateProfileRequest(null, tooLong, "testuser");
                mockMvc.perform(put("/users/me")
                                .header("Authorization", "Bearer " + authToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.errors[0].code").value("FULL_NAME_INVALID"));
        }

        @Test
        @DisplayName("Update profile: Fail — fullName blank (whitespace only)")
        void testUpdateProfile_Fail_FullNameBlank() throws Exception {
                UpdateProfileRequest request = new UpdateProfileRequest(null, "   ", "testuser");
                mockMvc.perform(put("/users/me")
                                .header("Authorization", "Bearer " + authToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.errors[0].code").value("FULL_NAME_INVALID"));
        }

        @Test
        @DisplayName("Update profile: Fail — username cooldown (changed within 30 days)")
        void testUpdateProfile_Fail_UsernameCooldown() throws Exception {
                testUser.setUsernameChangedAt(LocalDateTime.now().minusDays(5));
                userRepository.save(testUser);

                UpdateProfileRequest request = new UpdateProfileRequest(null, "Test User", "anotheruser");
                mockMvc.perform(put("/users/me")
                                .header("Authorization", "Bearer " + authToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.errors[0].code").value("CANNOT_CHANGE_USERNAME_YET"));
        }

        @Test
        @DisplayName("Update profile: Fail — username already exists (belongs to another user)")
        void testUpdateProfile_Fail_UsernameAlreadyExists() throws Exception {
                User anotherUser = User.builder()
                                .username("takenuser")
                                .email("another@gmail.com")
                                .passwordHash(passwordEncoder.encode(PASSWORD))
                                .fullName("Another User")
                                .isActive(true)
                                .isVerified(true)
                                .build();
                userRepository.save(anotherUser);

                UpdateProfileRequest request = new UpdateProfileRequest(null, "Test User", "takenuser");
                mockMvc.perform(put("/users/me")
                                .header("Authorization", "Bearer " + authToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.errors[0].code").value("USERNAME_EXISTED"));
        }
}
