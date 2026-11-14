package com.elingo.notification.publisher;

import com.elingo.common.dto.WsEventEnvelope;
import com.elingo.notification.dto.response.NotificationResponse;
import com.elingo.notification.entity.NotificationRefType;
import com.elingo.notification.entity.NotificationType;
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
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class NotificationEventPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationEventPublisher publisher;

    private static final Long RECIPIENT_ID = 100L;
    private NotificationResponse sampleNotification;

    @BeforeEach
    void setUp() {
        sampleNotification = new NotificationResponse(
                1L,
                NotificationType.POST_LIKED,
                "notification.POST_LIKED.title",
                "john_doe liked your post",
                Map.of("actor", "john_doe"),
                NotificationRefType.POST,
                200L,
                false,
                LocalDateTime.now()
        );
    }

    // ========================================================================
    // sendNotification(recipientId, notification)
    // ========================================================================
    @Nested
    @DisplayName("sendNotification")
    class SendNotificationTests {

        @Test
        @DisplayName("Send notification via websocket successfully")
        void sendNotification_Success() {
            assertThatCode(() -> publisher.sendNotification(RECIPIENT_ID, sampleNotification))
                    .doesNotThrowAnyException();

            ArgumentCaptor<WsEventEnvelope> envelopeCaptor = ArgumentCaptor.forClass(WsEventEnvelope.class);
            verify(messagingTemplate).convertAndSendToUser(
                    eq(String.valueOf(RECIPIENT_ID)),
                    eq("/queue/notifications"),
                    envelopeCaptor.capture()
            );

            WsEventEnvelope captured = envelopeCaptor.getValue();
            assertThat(captured.event()).isEqualTo(NotificationEvents.NOTIFICATION_NEW);
            assertThat(captured.data()).isEqualTo(sampleNotification);
        }

        @Test
        @DisplayName("Handle failure gracefully: MessagingException caught and does not crash")
        void sendNotification_Fail_MessagingException() {
            doThrow(new MessagingException("STOMP connection failed"))
                    .when(messagingTemplate).convertAndSendToUser(
                            eq(String.valueOf(RECIPIENT_ID)),
                            eq("/queue/notifications"),
                            any(WsEventEnvelope.class)
                    );

            assertThatCode(() -> publisher.sendNotification(RECIPIENT_ID, sampleNotification))
                    .doesNotThrowAnyException();

            verify(messagingTemplate).convertAndSendToUser(
                    eq(String.valueOf(RECIPIENT_ID)),
                    eq("/queue/notifications"),
                    any(WsEventEnvelope.class)
            );
        }
    }
}
