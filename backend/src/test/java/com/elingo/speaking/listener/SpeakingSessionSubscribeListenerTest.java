package com.elingo.speaking.listener;

import com.elingo.speaking.service.SpeakingConnectionService;
import com.elingo.speaking.state.SpeakingSessionRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SpeakingSessionSubscribeListenerTest {

    @Mock
    private SpeakingSessionRegistry sessionRegistry;

    @Mock
    private SpeakingConnectionService connectionService;

    @InjectMocks
    private SpeakingSessionSubscribeListener listener;

    private static final Long USER_ID = 1L;
    private static final String SESSION_ID = "session-1";
    private static final String SUBSCRIPTION_ID = "sub-0";
    private static final String SPEAKING_DESTINATION = "/user/queue/speaking";

    private final Principal user = () -> "1";

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
        @DisplayName("Subscribing to the user speaking queue registers the subscription")
        void handleSubscribe_Success() {
            listener.handleSubscribe(subscribeEvent(SPEAKING_DESTINATION, SUBSCRIPTION_ID, user));

            verify(sessionRegistry).addSubscription(USER_ID, SESSION_ID, SUBSCRIPTION_ID);
        }

        @Test
        @DisplayName("Subscribing to the other forms of the speaking queue destination registers the subscription")
        void handleSubscribe_Success_DestinationVariants() {
            listener.handleSubscribe(subscribeEvent("/queue/speaking", "sub-a", user));
            listener.handleSubscribe(subscribeEvent("/user/1/queue/speaking", "sub-b", user));

            verify(sessionRegistry).addSubscription(USER_ID, SESSION_ID, "sub-a");
            verify(sessionRegistry).addSubscription(USER_ID, SESSION_ID, "sub-b");
        }

        @Test
        @DisplayName("Subscribing without a subscription id registers a null subscription id")
        void handleSubscribe_Success_NullSubscriptionId() {
            listener.handleSubscribe(subscribeEvent(SPEAKING_DESTINATION, null, user));

            verify(sessionRegistry).addSubscription(USER_ID, SESSION_ID, null);
        }

        @Test
        @DisplayName("Subscribing to another destination is ignored")
        void handleSubscribe_Success_OtherDestinationIgnored() {
            listener.handleSubscribe(subscribeEvent("/user/queue/battle", SUBSCRIPTION_ID, user));

            verifyNoInteractions(sessionRegistry, connectionService);
        }

        @Test
        @DisplayName("Subscribing without a destination is ignored")
        void handleSubscribe_Success_NoDestinationIgnored() {
            listener.handleSubscribe(subscribeEvent(null, SUBSCRIPTION_ID, user));

            verifyNoInteractions(sessionRegistry, connectionService);
        }

        @Test
        @DisplayName("Subscribing as an anonymous user is ignored")
        void handleSubscribe_Success_NoUserIgnored() {
            listener.handleSubscribe(subscribeEvent(SPEAKING_DESTINATION, SUBSCRIPTION_ID, null));

            verifyNoInteractions(sessionRegistry, connectionService);
        }

        @Test
        @DisplayName("Subscribing with a user name that is not a number is ignored")
        void handleSubscribe_Success_InvalidUserNameIgnored() {
            listener.handleSubscribe(subscribeEvent(SPEAKING_DESTINATION, SUBSCRIPTION_ID, () -> "not-a-number"));

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
        @DisplayName("Unsubscribing the last speaking subscription marks the user offline")
        void handleUnsubscribe_Success_LastSubscription() {
            when(sessionRegistry.removeSubscription(SESSION_ID, SUBSCRIPTION_ID)).thenReturn(USER_ID);

            listener.handleUnsubscribe(unsubscribeEvent(SUBSCRIPTION_ID));

            verify(connectionService).onUserOffline(USER_ID);
        }

        @Test
        @DisplayName("Unsubscribing does nothing while the user still has another subscription or session")
        void handleUnsubscribe_Success_OtherSubscriptionRemains() {
            when(sessionRegistry.removeSubscription(SESSION_ID, SUBSCRIPTION_ID)).thenReturn(null);

            listener.handleUnsubscribe(unsubscribeEvent(SUBSCRIPTION_ID));

            verifyNoInteractions(connectionService);
        }

        @Test
        @DisplayName("Unsubscribing passes the session id and subscription id to the registry")
        void handleUnsubscribe_Success_UsesIds() {
            when(sessionRegistry.removeSubscription(SESSION_ID, "sub-7")).thenReturn(null);

            listener.handleUnsubscribe(unsubscribeEvent("sub-7"));

            verify(sessionRegistry).removeSubscription(SESSION_ID, "sub-7");
        }
    }
}
