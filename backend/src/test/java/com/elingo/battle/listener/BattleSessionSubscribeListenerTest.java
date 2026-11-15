package com.elingo.battle.listener;

import com.elingo.battle.service.BattleConnectionService;
import com.elingo.battle.service.BattleQueueService;
import com.elingo.battle.service.BattleRoomService;
import com.elingo.battle.state.BattleSessionRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class BattleSessionSubscribeListenerTest {

    @Mock
    private BattleSessionRegistry sessionRegistry;

    @Mock
    private BattleQueueService battleQueueService;

    @Mock
    private BattleRoomService battleRoomService;

    @Mock
    private BattleConnectionService battleConnectionService;

    @InjectMocks
    private BattleSessionSubscribeListener listener;

    private static final Long USER_ID = 1L;
    private static final String SESSION_ID = "session-1";
    private static final String SUBSCRIPTION_ID = "sub-0";
    private static final String BATTLE_DESTINATION = "/user/queue/battle";

    private Principal user = () -> "1";

    private SessionSubscribeEvent subscribeEvent(String destination, String subscriptionId, Principal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        accessor.setSessionId(SESSION_ID);
        if (subscriptionId != null) {
            accessor.setSubscriptionId(subscriptionId);
        }
        if (principal != null) {
            accessor.setUser(principal);
        }
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return new SessionSubscribeEvent(this, message, principal);
    }

    private SessionUnsubscribeEvent unsubscribeEvent(String subscriptionId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        accessor.setSessionId(SESSION_ID);
        accessor.setSubscriptionId(subscriptionId);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return new SessionUnsubscribeEvent(this, message, null);
    }

    // ========================================================================
    // handleSubscribe
    // ========================================================================
    @Nested
    @DisplayName("handleSubscribe")
    class HandleSubscribeTests {

        @Test
        @DisplayName("Subscribing to the user battle queue registers the subscription")
        void handleSubscribe_Success() {
            listener.handleSubscribe(subscribeEvent(BATTLE_DESTINATION, SUBSCRIPTION_ID, user));

            verify(sessionRegistry).addSubscription(USER_ID, SESSION_ID, SUBSCRIPTION_ID);
        }

        @Test
        @DisplayName("Subscribing to the other forms of the battle queue destination registers the subscription")
        void handleSubscribe_Success_DestinationVariants() {
            listener.handleSubscribe(subscribeEvent("/queue/battle", "sub-a", user));
            listener.handleSubscribe(subscribeEvent("/user/1/queue/battle", "sub-b", user));

            verify(sessionRegistry).addSubscription(USER_ID, SESSION_ID, "sub-a");
            verify(sessionRegistry).addSubscription(USER_ID, SESSION_ID, "sub-b");
        }

        @Test
        @DisplayName("Subscribing without a subscription id registers a null subscription id")
        void handleSubscribe_Success_NullSubscriptionId() {
            listener.handleSubscribe(subscribeEvent(BATTLE_DESTINATION, null, user));

            verify(sessionRegistry).addSubscription(USER_ID, SESSION_ID, null);
        }

        @Test
        @DisplayName("Subscribing to another destination is ignored")
        void handleSubscribe_Success_OtherDestinationIgnored() {
            listener.handleSubscribe(subscribeEvent("/user/queue/xp", SUBSCRIPTION_ID, user));
            listener.handleSubscribe(subscribeEvent("/topic/news", "sub-1", user));
            listener.handleSubscribe(subscribeEvent("/queue/battle-extra", "sub-2", user));

            verifyNoInteractions(sessionRegistry);
        }

        @Test
        @DisplayName("Subscribing without a destination is ignored")
        void handleSubscribe_Success_NoDestination() {
            listener.handleSubscribe(subscribeEvent(null, SUBSCRIPTION_ID, user));

            verifyNoInteractions(sessionRegistry);
        }

        @Test
        @DisplayName("Subscribing as an anonymous user is ignored")
        void handleSubscribe_Success_NoUser() {
            listener.handleSubscribe(subscribeEvent(BATTLE_DESTINATION, SUBSCRIPTION_ID, null));

            verifyNoInteractions(sessionRegistry);
        }

        @Test
        @DisplayName("Subscribing with a user name that is not a number is ignored")
        void handleSubscribe_Success_InvalidUserName() {
            listener.handleSubscribe(subscribeEvent(BATTLE_DESTINATION, SUBSCRIPTION_ID, () -> "not-a-number"));

            verify(sessionRegistry, never()).addSubscription(anyLong(), anyString(), any());
        }
    }

    // ========================================================================
    // handleUnsubscribe
    // ========================================================================
    @Nested
    @DisplayName("handleUnsubscribe")
    class HandleUnsubscribeTests {

        @Test
        @DisplayName("Unsubscribing the last battle subscription cleans up the queue, the room and the running match")
        void handleUnsubscribe_Success_LastSession() {
            when(sessionRegistry.removeSubscription(SESSION_ID, SUBSCRIPTION_ID)).thenReturn(USER_ID);

            listener.handleUnsubscribe(unsubscribeEvent(SUBSCRIPTION_ID));

            InOrder inOrder = inOrder(battleQueueService, battleRoomService, battleConnectionService);
            inOrder.verify(battleQueueService).removeOnDisconnect(USER_ID);
            inOrder.verify(battleRoomService).onDisconnect(USER_ID);
            inOrder.verify(battleConnectionService).onUserOffline(USER_ID);
        }

        @Test
        @DisplayName("Unsubscribing does nothing while the user still has another subscription or session")
        void handleUnsubscribe_Success_UserStillOnline() {
            when(sessionRegistry.removeSubscription(SESSION_ID, SUBSCRIPTION_ID)).thenReturn(null);

            listener.handleUnsubscribe(unsubscribeEvent(SUBSCRIPTION_ID));

            verify(sessionRegistry).removeSubscription(SESSION_ID, SUBSCRIPTION_ID);
            verifyNoInteractions(battleQueueService, battleRoomService, battleConnectionService);
        }
    }
}
