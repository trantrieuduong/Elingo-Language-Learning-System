package com.elingo.user.controller;

import com.elingo.user.dto.request.ChangePasswordRequest;
import com.elingo.user.dto.request.SendOTPUpdateEmailRequest;
import com.elingo.user.dto.request.SetPasswordRequest;
import com.elingo.user.dto.request.UpdateEmailRequest;
import com.elingo.user.dto.response.UserMeResponse;
import com.elingo.user.entity.Role;
import com.elingo.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import com.elingo.common.annotation.CurrentUserId;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(CurrentUserId.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return USER_ID;
                    }
                })
                .build();
    }

    @Nested
    @DisplayName("getMyInfo")
    class GetMyInfoTests {
        @Test
        @DisplayName("Get my info successfully")
        void getMyInfo_Success() throws Exception {
            UserMeResponse responseMock = new UserMeResponse(USER_ID, "testuser", "test@gmail.com", "Test User", null, Role.USER, true, false, null, null, true, false);
            when(userService.getMyInfo(USER_ID)).thenReturn(responseMock);

            mockMvc.perform(get("/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.username").value("testuser"));
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserByIdTests {
        @Test
        @DisplayName("Get user by id successfully")
        void getUserById_Success() throws Exception {
            UserMeResponse responseMock = new UserMeResponse(USER_ID, "testuser", "test@gmail.com", "Test User", null, Role.USER, true, false, null, null, true, false);
            when(userService.getUserById(USER_ID, USER_ID)).thenReturn(responseMock);

            mockMvc.perform(get("/users/{id}", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.username").value("testuser"));
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePasswordTests {
        @Test
        @DisplayName("Change password successfully")
        void changePassword_Success() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest("OldPassword123@", "NewPassword123@");

            mockMvc.perform(patch("/users/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(userService).changePassword(eq(USER_ID), any(ChangePasswordRequest.class));
        }
    }

    @Nested
    @DisplayName("setPassword")
    class SetPasswordTests {
        @Test
        @DisplayName("Set password successfully")
        void setPassword_Success() throws Exception {
            SetPasswordRequest request = new SetPasswordRequest("NewPassword123@");

            mockMvc.perform(post("/users/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(userService).setPassword(eq(USER_ID), any(SetPasswordRequest.class));
        }
    }

    @Nested
    @DisplayName("sendOTPUpdateEmail")
    class SendOTPUpdateEmailTests {
        @Test
        @DisplayName("Send OTP successfully")
        void sendOTPUpdateEmail_Success() throws Exception {
            SendOTPUpdateEmailRequest request = new SendOTPUpdateEmailRequest("new@gmail.com");

            mockMvc.perform(post("/users/me/email/otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(userService).sendOTPUpdateEmail(eq(USER_ID), any(SendOTPUpdateEmailRequest.class));
        }
    }

    @Nested
    @DisplayName("updateEmail")
    class UpdateEmailTests {
        @Test
        @DisplayName("Update email successfully")
        void updateEmail_Success() throws Exception {
            UpdateEmailRequest request = new UpdateEmailRequest("new@gmail.com", "123456", "Password123@");

            mockMvc.perform(patch("/users/me/email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(userService).updateEmail(eq(USER_ID), any(UpdateEmailRequest.class));
        }
    }
}
