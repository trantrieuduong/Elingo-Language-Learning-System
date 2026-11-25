package com.elingo.speaking.listener;

import com.elingo.speaking.service.SpeakingConnectionService;
import com.elingo.speaking.state.SpeakingSessionRegistry;
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SpeakingSessionDisconnectListenerTest {

    @Mock
    private SpeakingSessionRegistry sessionRegistry;

    @Mock
    private SpeakingConnectionService connectionService;

    @InjectMocks
    private SpeakingSessionDisconnectListener listener;

    private static final Long USER_ID = 1L;
    private static final String SESSION_ID = "session-1";

    private SessionDisconnectEvent buildEvent(Principal user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setSessionId(SESSION_ID);
        if (user != null) {
            accessor.setUser(user);
        }
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return new SessionDisconnectEvent(this, message, SESSION_ID, CloseStatus.NORMAL, user);
    }

    @Test
    @DisplayName("Disconnect of the last session marks the user offline")
    void handleDisconnect_Success_LastSession() {
        when(sessionRegistry.removeAndCheckLast(USER_ID, SESSION_ID)).thenReturn(true);

        listener.handleDisconnect(buildEvent(() -> "1"));

        verify(connectionService).onUserOffline(USER_ID);
    }

    @Test
    @DisplayName("Disconnect of a session does nothing while the user still has another session")
    void handleDisconnect_Success_OtherSessionRemains() {
        when(sessionRegistry.removeAndCheckLast(USER_ID, SESSION_ID)).thenReturn(false);

        listener.handleDisconnect(buildEvent(() -> "1"));

        verifyNoInteractions(connectionService);
    }

    @Test
    @DisplayName("Disconnect of an anonymous session is ignored")
    void handleDisconnect_Success_NoUser() {
        listener.handleDisconnect(buildEvent(null));

        verifyNoInteractions(sessionRegistry, connectionService);
    }

    @Test
    @DisplayName("Disconnect with a user name that is not a number is ignored")
    void handleDisconnect_Success_InvalidUserName() {
        listener.handleDisconnect(buildEvent(() -> "not-a-number"));

        verifyNoInteractions(sessionRegistry, connectionService);
    }

    @Test
    @DisplayName("Disconnect passes the closed session id to the registry")
    void handleDisconnect_Success_UsesSessionId() {
        when(sessionRegistry.removeAndCheckLast(USER_ID, SESSION_ID)).thenReturn(false);

        listener.handleDisconnect(buildEvent(() -> "1"));

        verify(sessionRegistry).removeAndCheckLast(USER_ID, SESSION_ID);
    }
}
