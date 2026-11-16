package com.elingo.gamification.listener;

import com.elingo.common.event.LevelUpEvent;
import com.elingo.gamification.catalog.LevelCatalog;
import com.elingo.gamification.dto.response.LevelUpWsResponse;
import com.elingo.gamification.publisher.GamificationEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("LevelUpListener Tests")
class LevelUpListenerTest {

    @Mock
    private LevelCatalog levelCatalog;

    @Mock
    private GamificationEventPublisher gamificationEventPublisher;

    @InjectMocks
    private LevelUpListener levelUpListener;

    private static final Long USER_ID = 10L;
    private static final int NEW_LEVEL = 3;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 16, 19, 30);

    @Nested
    @DisplayName("on")
    class OnLevelUpEventTests {

        @Test
        @DisplayName("Should lookup title from catalog and push level up websocket event successfully")
        void on_Success() {
            LevelUpEvent event = new LevelUpEvent(USER_ID, NEW_LEVEL, NOW);
            when(levelCatalog.getTitleForLevel(NEW_LEVEL)).thenReturn("Challenger");

            levelUpListener.on(event);

            ArgumentCaptor<LevelUpWsResponse> captor = ArgumentCaptor.forClass(LevelUpWsResponse.class);
            verify(gamificationEventPublisher).sendLevelUp(eq(USER_ID), captor.capture());

            LevelUpWsResponse captured = captor.getValue();
            assertThat(captured.newLevel()).isEqualTo(NEW_LEVEL);
            assertThat(captured.title()).isEqualTo("Challenger");
            assertThat(captured.unlockedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("Should handle exception gracefully when publisher throws error")
        void on_WhenPublisherThrows_HandlesGracefully() {
            LevelUpEvent event = new LevelUpEvent(USER_ID, NEW_LEVEL, NOW);
            when(levelCatalog.getTitleForLevel(NEW_LEVEL)).thenReturn("Challenger");
            doThrow(new RuntimeException("WebSocket connection broken"))
                    .when(gamificationEventPublisher).sendLevelUp(eq(USER_ID), any());

            assertDoesNotThrow(() -> levelUpListener.on(event));

            verify(gamificationEventPublisher).sendLevelUp(eq(USER_ID), any());
        }
    }
}
