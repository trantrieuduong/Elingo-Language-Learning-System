package com.elingo.notification.listener;

import com.elingo.common.event.NotificationEvent;
import com.elingo.notification.dto.response.NotificationResponse;
import com.elingo.notification.entity.NotificationRefType;
import com.elingo.notification.entity.NotificationType;
import com.elingo.notification.service.NotificationService;
import com.elingo.notification.publisher.NotificationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @InjectMocks
    private NotificationEventListener notificationEventListener;

    private static final Long RECIPIENT_ID = 100L;
    private static final Long REF_ID = 200L;
    private static final String ACTOR_NAME = "john_doe";

    private NotificationResponse sampleResponse;

    @BeforeEach
    void setUp() {
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
    // on(NotificationEvent)
    // ========================================================================
    @Nested
    @DisplayName("on")
    class OnNotificationEventTests {

        @Test
        @DisplayName("Process notification successfully: Save to database and push to websocket")
        void on_Success() {
            NotificationEvent event = NotificationEvent.postLiked(RECIPIENT_ID, REF_ID, ACTOR_NAME);
            when(notificationService.create(event)).thenReturn(sampleResponse);

            assertThatCode(() -> notificationEventListener.on(event))
                    .doesNotThrowAnyException();

            verify(notificationService).create(event);
            verify(notificationEventPublisher).sendNotification(RECIPIENT_ID, sampleResponse);
        }

        @Test
        @DisplayName("Process badge earned notification successfully")
        void on_Success_BadgeEarned() {
            NotificationEvent event = NotificationEvent.badgeEarned(RECIPIENT_ID, 300L, "Fast Learner");
            NotificationResponse badgeResponse = new NotificationResponse(
                    2L,
                    NotificationType.BADGE_EARNED,
                    "notification.BADGE_EARNED.title",
                    "You earned the Fast Learner badge",
                    Map.of("badgeName", "Fast Learner"),
                    NotificationRefType.BADGE,
                    300L,
                    false,
                    LocalDateTime.now()
            );

            when(notificationService.create(event)).thenReturn(badgeResponse);

            assertThatCode(() -> notificationEventListener.on(event))
                    .doesNotThrowAnyException();

            verify(notificationService).create(event);
            verify(notificationEventPublisher).sendNotification(RECIPIENT_ID, badgeResponse);
        }

        @Test
        @DisplayName("Handle failure gracefully: Service throws exception, swallows and does not push websocket")
        void on_Fail_ServiceThrowsException() {
            NotificationEvent event = NotificationEvent.postLiked(RECIPIENT_ID, REF_ID, ACTOR_NAME);
            when(notificationService.create(event)).thenThrow(new RuntimeException("Database connection lost"));

            assertThatCode(() -> notificationEventListener.on(event))
                    .doesNotThrowAnyException();

            verify(notificationService).create(event);
            verifyNoInteractions(notificationEventPublisher);
        }

        @Test
        @DisplayName("Handle failure gracefully: WebSocket publisher throws exception, swallows and does not crash")
        void on_Fail_PublisherThrowsException() {
            NotificationEvent event = NotificationEvent.postLiked(RECIPIENT_ID, REF_ID, ACTOR_NAME);
            when(notificationService.create(event)).thenReturn(sampleResponse);
            doThrow(new RuntimeException("STOMP session closed"))
                    .when(notificationEventPublisher).sendNotification(RECIPIENT_ID, sampleResponse);

            assertThatCode(() -> notificationEventListener.on(event))
                    .doesNotThrowAnyException();

            verify(notificationService).create(event);
            verify(notificationEventPublisher).sendNotification(RECIPIENT_ID, sampleResponse);
        }
    }
}
