package com.elingo.user.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.CursorResponse;
import com.elingo.common.service.RateLimiterService;
import com.elingo.community.dto.response.AuthorResponse;
import com.elingo.community.dto.response.PostResponse;
import com.elingo.community.entity.PostStatus;
import com.elingo.community.service.PostService;
import com.elingo.user.dto.request.ChangePasswordRequest;
import com.elingo.user.dto.request.SendOTPUpdateEmailRequest;
import com.elingo.user.dto.request.SetPasswordRequest;
import com.elingo.user.dto.request.UpdateEmailRequest;
import com.elingo.user.dto.request.UpdateProfileRequest;
import com.elingo.user.dto.response.UserMeResponse;
import com.elingo.user.entity.Role;
import com.elingo.user.service.UserService;
import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
import com.elingo.vocabulary.service.DeckService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private DeckService deckService;

    @Mock
    private PostService postService;

    @Mock
    private RateLimiterService limiter;

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
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
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
            UserMeResponse responseMock = new UserMeResponse(USER_ID, "testuser", "test@gmail.com", "Test User", null,
                    Role.USER, true, false, null, null, true, false);
            when(userService.getMyInfo(USER_ID)).thenReturn(responseMock);

            mockMvc.perform(get("/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.username").value("testuser"));
        }
    }

    @Nested
    @DisplayName("getMyDecks")
    class GetMyDecksTests {

        private DeckResponse buildDeckResponse(Long id, String title) {
            return new DeckResponse(
                    id,
                    title,
                    "deck-slug",
                    "Deck description",
                    false,
                    null,
                    DeckStatus.DRAFT,
                    OwnerType.USER,
                    USER_ID,
                    0,
                    0,
                    0,
                    null,
                    Set.of(),
                    Set.of()
            );
        }

        @Test
        @DisplayName("Get my decks successfully")
        void getMyDecks_Success() throws Exception {
            DeckResponse deck = buildDeckResponse(10L, "My Vocabulary Deck");
            when(deckService.getUserDecks(USER_ID)).thenReturn(List.of(deck));

            mockMvc.perform(get("/users/me/decks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].id").value(10))
                    .andExpect(jsonPath("$.data[0].title").value("My Vocabulary Deck"));

            verify(deckService).getUserDecks(USER_ID);
        }

        @Test
        @DisplayName("Get my decks successfully when empty")
        void getMyDecks_Success_Empty() throws Exception {
            when(deckService.getUserDecks(USER_ID)).thenReturn(List.of());

            mockMvc.perform(get("/users/me/decks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(deckService).getUserDecks(USER_ID);
        }
    }

    @Nested
    @DisplayName("getUserPosts")
    class GetUserPostsTests {

        private PostResponse buildPostResponse(Long id) {
            return new PostResponse(
                    id,
                    1L,
                    "Post Title",
                    "Post Content",
                    PostStatus.PUBLISHED,
                    0,
                    0,
                    false,
                    new AuthorResponse(USER_ID, "testuser", "Test User", null),
                    List.of(),
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
        }

        @Test
        @DisplayName("Get user posts successfully with default parameters")
        void getUserPosts_Success_DefaultParams() throws Exception {
            PostResponse post = buildPostResponse(100L);
            CursorResponse<PostResponse> responseMock = new CursorResponse<>(List.of(post), "cursorNext", true);

            when(postService.getUserPosts(USER_ID, null, 20, USER_ID)).thenReturn(responseMock);

            mockMvc.perform(get("/users/{id}/posts", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items.length()").value(1))
                    .andExpect(jsonPath("$.data.items[0].id").value(100))
                    .andExpect(jsonPath("$.data.nextCursor").value("cursorNext"))
                    .andExpect(jsonPath("$.data.hasMore").value(true));

            verify(postService).getUserPosts(USER_ID, null, 20, USER_ID);
        }

        @Test
        @DisplayName("Get user posts successfully with custom query parameters")
        void getUserPosts_Success_CustomParams() throws Exception {
            Long targetUserId = 2L;
            CursorResponse<PostResponse> responseMock = new CursorResponse<>(List.of(), null, false);

            when(postService.getUserPosts(targetUserId, "cursor123", 10, USER_ID)).thenReturn(responseMock);

            mockMvc.perform(get("/users/{id}/posts", targetUserId)
                            .param("cursor", "cursor123")
                            .param("limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items.length()").value(0))
                    .andExpect(jsonPath("$.data.hasMore").value(false));

            verify(postService).getUserPosts(targetUserId, "cursor123", 10, USER_ID);
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserByIdTests {
        @Test
        @DisplayName("Get user by id successfully")
        void getUserById_Success() throws Exception {
            UserMeResponse responseMock = new UserMeResponse(USER_ID, "testuser", "test@gmail.com", "Test User", null,
                    Role.USER, true, false, null, null, true, false);
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

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfileTests {
        @Test
        @DisplayName("Update profile successfully")
        void updateProfile_Success() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest(null, "New Name", "newuser");
            UserMeResponse responseMock = new UserMeResponse(USER_ID, "newuser", "test@gmail.com", "New Name", null,
                    Role.USER, true, false, null, null, true, false);
            when(userService.updateProfile(USER_ID, request)).thenReturn(responseMock);

            mockMvc.perform(put("/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.username").value("newuser"))
                    .andExpect(jsonPath("$.data.fullName").value("New Name"));

            verify(userService).updateProfile(eq(USER_ID), any(UpdateProfileRequest.class));
        }
    }
}
