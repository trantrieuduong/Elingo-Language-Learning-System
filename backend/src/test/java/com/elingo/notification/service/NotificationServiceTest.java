package com.elingo.notification.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.common.event.NotificationEvent;
import com.elingo.notification.dto.response.UnreadCountResponse;
import com.elingo.notification.dto.response.NotificationResponse;
import com.elingo.notification.entity.Notification;
import com.elingo.notification.entity.NotificationRefType;
import com.elingo.notification.entity.NotificationType;
import com.elingo.notification.mapper.NotificationMapper;
import com.elingo.notification.repository.NotificationRepository;
import com.elingo.notification.service.impl.NotificationServiceImpl;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper mapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private static final Long USER_ID = 100L;
    private static final Long REF_ID = 200L;
    private static final String ACTOR_NAME = "john_doe";

    private User testUser;
    private Notification sampleNotification;
    private NotificationResponse sampleResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(USER_ID).build();

        sampleNotification = Notification.builder()
                .id(1L)
                .user(testUser)
                .type(NotificationType.POST_LIKED)
                .title("john_doe liked your post")
                .params(Map.of("actor", ACTOR_NAME))
                .refType(NotificationRefType.POST)
                .refId(REF_ID)
                .isRead(false)
                .build();

        sampleResponse = new NotificationResponse(
                1L,
                NotificationType.POST_LIKED,
                "notification.POST_LIKED.title",
                "john_doe liked your post",
                Map.of("actor", ACTOR_NAME),
                NotificationRefType.POST,
                REF_ID,
                false,
                LocalDateTime.now()
        );
    }

    // ========================================================================
    // create(NotificationEvent)
    // ========================================================================
    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("Create notification successfully from PostLiked event")
        void create_Success() {
            NotificationEvent event = NotificationEvent.postLiked(USER_ID, REF_ID, ACTOR_NAME);

            when(userRepository.getReferenceById(USER_ID)).thenReturn(testUser);
            when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);
            when(mapper.toResponse(sampleNotification)).thenReturn(sampleResponse);

            NotificationResponse result = notificationService.create(event);

            assertThat(result).isEqualTo(sampleResponse);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            Notification captured = captor.getValue();
            assertThat(captured.getUser()).isEqualTo(testUser);
            assertThat(captured.getType()).isEqualTo(NotificationType.POST_LIKED);
            assertThat(captured.getTitle()).isEqualTo("john_doe liked your post");
            assertThat(captured.getRefType()).isEqualTo(NotificationRefType.POST);
            assertThat(captured.getRefId()).isEqualTo(REF_ID);
            assertThat(captured.getParams()).containsEntry("actor", ACTOR_NAME);
        }

        @Test
        @DisplayName("Create notification successfully from SubscriptionActivated event")
        void create_Success_SubscriptionActivated() {
            NotificationEvent event = NotificationEvent.subscriptionActivated(USER_ID, 300L, "PRO", "2026-12-31");
            Notification subNotification = Notification.builder()
                    .id(2L)
                    .user(testUser)
                    .type(NotificationType.SUBSCRIPTION_ACTIVATED)
                    .title("Your PRO plan is active until 2026-12-31")
                    .params(Map.of("planName", "PRO", "expiresAt", "2026-12-31"))
                    .refType(NotificationRefType.SUBSCRIPTION)
                    .refId(300L)
                    .isRead(false)
                    .build();
            NotificationResponse subResponse = new NotificationResponse(
                    2L,
                    NotificationType.SUBSCRIPTION_ACTIVATED,
                    "notification.SUBSCRIPTION_ACTIVATED.title",
                    "Your PRO plan is active until 2026-12-31",
                    Map.of("planName", "PRO", "expiresAt", "2026-12-31"),
                    NotificationRefType.SUBSCRIPTION,
                    300L,
                    false,
                    LocalDateTime.now()
            );

            when(userRepository.getReferenceById(USER_ID)).thenReturn(testUser);
            when(notificationRepository.save(any(Notification.class))).thenReturn(subNotification);
            when(mapper.toResponse(subNotification)).thenReturn(subResponse);

            NotificationResponse result = notificationService.create(event);

            assertThat(result).isEqualTo(subResponse);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            Notification captured = captor.getValue();
            assertThat(captured.getUser()).isEqualTo(testUser);
            assertThat(captured.getType()).isEqualTo(NotificationType.SUBSCRIPTION_ACTIVATED);
            assertThat(captured.getTitle()).isEqualTo("Your PRO plan is active until 2026-12-31");
            assertThat(captured.getRefType()).isEqualTo(NotificationRefType.SUBSCRIPTION);
            assertThat(captured.getRefId()).isEqualTo(300L);
            assertThat(captured.getParams()).containsEntry("planName", "PRO");
            assertThat(captured.getParams()).containsEntry("expiresAt", "2026-12-31");
        }

        @Test
        @DisplayName("Create notification failed: UserRepository throws exception")
        void create_Fail_UserNotFound() {
            NotificationEvent event = NotificationEvent.postLiked(USER_ID, REF_ID, ACTOR_NAME);
            when(userRepository.getReferenceById(USER_ID))
                    .thenThrow(new RuntimeException("User reference error"));

            assertThatThrownBy(() -> notificationService.create(event))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User reference error");

            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Create notification failed: Repository save throws exception")
        void create_Fail_SaveFailed() {
            NotificationEvent event = NotificationEvent.postLiked(USER_ID, REF_ID, ACTOR_NAME);
            when(userRepository.getReferenceById(USER_ID)).thenReturn(testUser);
            when(notificationRepository.save(any(Notification.class)))
                    .thenThrow(new RuntimeException("Database unique constraint violation"));

            assertThatThrownBy(() -> notificationService.create(event))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database unique constraint violation");
        }
    }

    // ========================================================================
    // getAll(userId, pageable)
    // ========================================================================
    @Nested
    @DisplayName("getAll")
    class GetAllTests {

        @Test
        @DisplayName("Get all notifications successfully with items")
        void getAll_Success_WithItems() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Notification> page = new PageImpl<>(List.of(sampleNotification), pageable, 1);

            when(notificationRepository.findByUserId(USER_ID, pageable)).thenReturn(page);
            when(mapper.toResponse(sampleNotification)).thenReturn(sampleResponse);

            PageResponse<NotificationResponse> result = notificationService.getAll(USER_ID, pageable);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().getFirst()).isEqualTo(sampleResponse);
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.page()).isEqualTo(1);
            verify(notificationRepository).findByUserId(USER_ID, pageable);
        }

        @Test
        @DisplayName("Get all notifications successfully when user has no notifications")
        void getAll_Success_EmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Notification> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(notificationRepository.findByUserId(USER_ID, pageable)).thenReturn(emptyPage);

            PageResponse<NotificationResponse> result = notificationService.getAll(USER_ID, pageable);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
            verify(notificationRepository).findByUserId(USER_ID, pageable);
            verifyNoInteractions(mapper);
        }

        @Test
        @DisplayName("Get all notifications failed: Repository throws exception")
        void getAll_Fail_RepositoryError() {
            Pageable pageable = PageRequest.of(0, 10);
            when(notificationRepository.findByUserId(USER_ID, pageable))
                    .thenThrow(new RuntimeException("Connection timeout"));

            assertThatThrownBy(() -> notificationService.getAll(USER_ID, pageable))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Connection timeout");
        }
    }

    // ========================================================================
    // countUnread(userId)
    // ========================================================================
    @Nested
    @DisplayName("countUnread")
    class CountUnreadTests {

        @Test
        @DisplayName("Count unread successfully when user has unread notifications")
        void countUnread_Success_HasUnread() {
            when(notificationRepository.countByUserIdAndIsReadIsFalse(USER_ID)).thenReturn(5L);

            UnreadCountResponse response = notificationService.countUnread(USER_ID);

            assertThat(response.count()).isEqualTo(5L);
            verify(notificationRepository).countByUserIdAndIsReadIsFalse(USER_ID);
        }

        @Test
        @DisplayName("Count unread successfully when all notifications are read")
        void countUnread_Success_ZeroUnread() {
            when(notificationRepository.countByUserIdAndIsReadIsFalse(USER_ID)).thenReturn(0L);

            UnreadCountResponse response = notificationService.countUnread(USER_ID);

            assertThat(response.count()).isZero();
            verify(notificationRepository).countByUserIdAndIsReadIsFalse(USER_ID);
        }

        @Test
        @DisplayName("Count unread failed: Repository throws exception")
        void countUnread_Fail_RepositoryError() {
            when(notificationRepository.countByUserIdAndIsReadIsFalse(USER_ID))
                    .thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> notificationService.countUnread(USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB error");
        }
    }

    // ========================================================================
    // markRead(userId, notificationId)
    // ========================================================================
    @Nested
    @DisplayName("markRead")
    class MarkReadTests {

        @Test
        @DisplayName("Mark notification as read successfully")
        void markRead_Success() {
            Long notificationId = 1L;

            notificationService.markRead(USER_ID, notificationId);

            verify(notificationRepository).markRead(USER_ID, notificationId);
        }

        @Test
        @DisplayName("Mark notification as read failed: Repository throws exception")
        void markRead_Fail_RepositoryError() {
            Long notificationId = 1L;
            doThrow(new RuntimeException("Lock wait timeout"))
                    .when(notificationRepository).markRead(USER_ID, notificationId);

            assertThatThrownBy(() -> notificationService.markRead(USER_ID, notificationId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Lock wait timeout");
        }
    }

    // ========================================================================
    // markAllRead(userId)
    // ========================================================================
    @Nested
    @DisplayName("markAllRead")
    class MarkAllReadTests {

        @Test
        @DisplayName("Mark all notifications as read successfully")
        void markAllRead_Success() {
            notificationService.markAllRead(USER_ID);

            verify(notificationRepository).markAllRead(USER_ID);
        }

        @Test
        @DisplayName("Mark all notifications as read failed: Repository throws exception")
        void markAllRead_Fail_RepositoryError() {
            doThrow(new RuntimeException("DB transaction error"))
                    .when(notificationRepository).markAllRead(USER_ID);

            assertThatThrownBy(() -> notificationService.markAllRead(USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB transaction error");
        }
    }
}
