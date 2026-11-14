package com.elingo.battle.websocket;

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
class BattleEventPublisherTest {

    private static final String BATTLE_QUEUE = "/queue/battle";
    private static final String ERROR_EVENT = "error";

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private BattleEventPublisher publisher;

    private static final Long USER_ID = 1L;

    /**
     * WsEventEnvelope có trường timestamp = System.currentTimeMillis() nên không thể so sánh equals
     * với một envelope tạo sẵn -> bắt envelope thực tế rồi assert từng trường.
     */
    private WsEventEnvelope<?> captureEnvelope(String expectedUser) {
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq(expectedUser), eq(BATTLE_QUEUE), payloadCaptor.capture());
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
        @DisplayName("Send event wraps the payload in an envelope and sends it to the user battle queue")
        void sendToUser_Success() {
            Map<String, Object> data = Map.of("mode", "TYPING");
            long before = System.currentTimeMillis();

            publisher.sendToUser(USER_ID, BattleEvents.QUEUE_JOINED, data);

            WsEventEnvelope<?> envelope = captureEnvelope("1");
            assertThat(envelope.event()).isEqualTo(BattleEvents.QUEUE_JOINED);
            assertThat(envelope.data()).isEqualTo(data);
            assertThat(envelope.timestamp()).isBetween(before, System.currentTimeMillis());
            verifyNoMoreInteractions(messagingTemplate);
        }

        @Test
        @DisplayName("Send event uses the user id as the STOMP user name")
        void sendToUser_UserName() {
            publisher.sendToUser(42L, BattleEvents.QUEUE_LEFT, Collections.emptyMap());

            WsEventEnvelope<?> envelope = captureEnvelope("42");
            assertThat(envelope.event()).isEqualTo(BattleEvents.QUEUE_LEFT);
            assertThat(envelope.data()).isEqualTo(Collections.emptyMap());
        }

        @Test
        @DisplayName("Send event allows a null payload")
        void sendToUser_NullPayload() {
            publisher.sendToUser(USER_ID, BattleEvents.MATCH_REJOINED, null);

            WsEventEnvelope<?> envelope = captureEnvelope("1");
            assertThat(envelope.event()).isEqualTo(BattleEvents.MATCH_REJOINED);
            assertThat(envelope.data()).isNull();
        }

        @Test
        @DisplayName("Send event without payload sends an envelope with null data")
        void sendToUser_NoPayloadOverload() {
            publisher.sendToUser(USER_ID, BattleEvents.QUEUE_LEFT);

            WsEventEnvelope<?> envelope = captureEnvelope("1");
            assertThat(envelope.event()).isEqualTo(BattleEvents.QUEUE_LEFT);
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
            publisher.sendError(USER_ID, "BATTLE_ALREADY_PARTICIPATED", "Already in a match");

            WsEventEnvelope<?> envelope = captureEnvelope("1");
            assertThat(envelope.event()).isEqualTo(ERROR_EVENT);
            assertThat(envelope.data())
                    .isEqualTo(new WsErrorPayload("BATTLE_ALREADY_PARTICIPATED", "Already in a match"));
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
        @DisplayName("Destination prefix is the battle application prefix")
        void destinationPrefix() {
            assertThat(publisher.destinationPrefix()).isEqualTo("/app/battle");
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
