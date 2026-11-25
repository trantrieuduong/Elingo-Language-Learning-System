package com.elingo.speaking.websocket;

import com.elingo.common.dto.WsErrorPayload;
import com.elingo.common.dto.WsEventEnvelope;
import com.elingo.common.websocket.AbstractWsSender;
import com.elingo.common.websocket.WsErrorSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SpeakingEventPublisherTest {

    private static final String SPEAKING_QUEUE = "/queue/speaking";
    private static final Long USER_ID = 1L;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private SpeakingEventPublisher publisher;

    /**
     * WsEventEnvelope có trường timestamp = System.currentTimeMillis() nên không thể so sánh equals
     * với một envelope tạo sẵn -> bắt envelope thực tế rồi assert từng trường.
     */
    private WsEventEnvelope<?> captureEnvelope(String expectedUser) {
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq(expectedUser), eq(SPEAKING_QUEUE), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).isInstanceOf(WsEventEnvelope.class);
        return (WsEventEnvelope<?>) payloadCaptor.getValue();
    }

    // ========================================================================
    // sendToUser
    // ========================================================================
    @Nested
    @DisplayName("sendToUser")
    class SendToUserTests {

        @Test
        @DisplayName("Send event wraps the payload in an envelope and sends it to the user speaking queue")
        void sendToUser_Success() {
            Map<String, Object> data = Map.of("roomCode", "ABC234");
            long before = System.currentTimeMillis();

            publisher.sendToUser(USER_ID, SpeakingEvents.ROOM_CREATED, data);

            WsEventEnvelope<?> envelope = captureEnvelope("1");
            assertThat(envelope.event()).isEqualTo(SpeakingEvents.ROOM_CREATED);
            assertThat(envelope.data()).isEqualTo(data);
            assertThat(envelope.timestamp()).isBetween(before, System.currentTimeMillis());
            verifyNoMoreInteractions(messagingTemplate);
        }

        @Test
        @DisplayName("Send event uses the user id as the STOMP user name")
        void sendToUser_UserName() {
            publisher.sendToUser(42L, SpeakingEvents.PEER_LEFT, Collections.emptyMap());

            WsEventEnvelope<?> envelope = captureEnvelope("42");
            assertThat(envelope.event()).isEqualTo(SpeakingEvents.PEER_LEFT);
            assertThat(envelope.data()).isEqualTo(Collections.emptyMap());
        }

        @Test
        @DisplayName("Send event allows a null payload")
        void sendToUser_NullPayload() {
            publisher.sendToUser(USER_ID, SpeakingEvents.ROOM_ACTIVE, null);

            WsEventEnvelope<?> envelope = captureEnvelope("1");
            assertThat(envelope.event()).isEqualTo(SpeakingEvents.ROOM_ACTIVE);
            assertThat(envelope.data()).isNull();
        }

        @Test
        @DisplayName("Send event without payload sends an envelope with null data")
        void sendToUser_NoPayloadOverload() {
            publisher.sendToUser(USER_ID, SpeakingEvents.ROOM_KICKED);

            WsEventEnvelope<?> envelope = captureEnvelope("1");
            assertThat(envelope.event()).isEqualTo(SpeakingEvents.ROOM_KICKED);
            assertThat(envelope.data()).isNull();
        }
    }

    // ========================================================================
    // sendError
    // ========================================================================
    @Nested
    @DisplayName("sendError")
    class SendErrorTests {

        @Test
        @DisplayName("Send error sends an error event with the code and message")
        void sendError_Success() {
            publisher.sendError(USER_ID, "SPEAKING_ROOM_FULL", "Room is full");

            WsEventEnvelope<?> envelope = captureEnvelope("1");
            assertThat(envelope.event()).isEqualTo(SpeakingEvents.ERROR);
            assertThat(envelope.data()).isEqualTo(new WsErrorPayload("SPEAKING_ROOM_FULL", "Room is full"));
            verifyNoMoreInteractions(messagingTemplate);
        }
    }

    // ========================================================================
    // destinationPrefix
    // ========================================================================
    @Nested
    @DisplayName("destinationPrefix")
    class DestinationPrefixTests {

        @Test
        @DisplayName("Destination prefix is the speaking application prefix")
        void destinationPrefix() {
            assertThat(publisher.destinationPrefix()).isEqualTo("/app/speaking");
        }

        @Test
        @DisplayName("Publisher can be used as a WebSocket error sender")
        void isWsErrorSender() {
            assertThat(publisher).isInstanceOf(WsErrorSender.class);
        }

        @Test
        @DisplayName("Publisher reuses the shared abstract WebSocket sender")
        void isAbstractWsSender() {
            assertThat(publisher).isInstanceOf(AbstractWsSender.class);
        }
    }
}
