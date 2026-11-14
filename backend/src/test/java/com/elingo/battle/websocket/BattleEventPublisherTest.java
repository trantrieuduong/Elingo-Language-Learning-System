package com.elingo.battle.websocket;

import com.elingo.battle.dto.response.BattleErrorResponse;
import com.elingo.battle.dto.response.BattleEventEnvelope;
import com.elingo.common.websocket.WsErrorSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class BattleEventPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private BattleEventPublisher publisher;

    private static final Long USER_ID = 1L;

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

            publisher.sendToUser(USER_ID, BattleEvents.QUEUE_JOINED, data);

            verify(messagingTemplate).convertAndSendToUser(
                    "1", "/queue/battle", new BattleEventEnvelope(BattleEvents.QUEUE_JOINED, data));
            verifyNoMoreInteractions(messagingTemplate);
        }

        @Test
        @DisplayName("Send event uses the user id as the STOMP user name")
        void sendToUser_UserName() {
            publisher.sendToUser(42L, BattleEvents.QUEUE_LEFT, Collections.emptyMap());

            verify(messagingTemplate).convertAndSendToUser(
                    "42", "/queue/battle", new BattleEventEnvelope(BattleEvents.QUEUE_LEFT, Collections.emptyMap()));
        }

        @Test
        @DisplayName("Send event allows a null payload")
        void sendToUser_NullPayload() {
            publisher.sendToUser(USER_ID, BattleEvents.MATCH_REJOINED, null);

            verify(messagingTemplate).convertAndSendToUser(
                    "1", "/queue/battle", new BattleEventEnvelope(BattleEvents.MATCH_REJOINED, null));
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

            verify(messagingTemplate).convertAndSendToUser(
                    "1", "/queue/battle",
                    new BattleEventEnvelope(BattleEvents.ERROR,
                            new BattleErrorResponse("BATTLE_ALREADY_PARTICIPATED", "Already in a match")));
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
    }
}
