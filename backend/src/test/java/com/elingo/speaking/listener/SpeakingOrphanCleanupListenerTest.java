package com.elingo.speaking.listener;

import com.elingo.speaking.service.SpeakingCleanupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SpeakingOrphanCleanupListenerTest {

    @Mock
    private SpeakingCleanupService cleanupService;

    @InjectMocks
    private SpeakingOrphanCleanupListener listener;

    @Test
    @DisplayName("On ready closes the open rooms left from the previous run")
    void onReady_Success() {
        when(cleanupService.cleanAllOnStartup()).thenReturn(3);

        assertThatCode(() -> listener.onReady()).doesNotThrowAnyException();

        verify(cleanupService).cleanAllOnStartup();
    }

    @Test
    @DisplayName("On ready succeeds when there is nothing to close")
    void onReady_Success_NothingToClose() {
        when(cleanupService.cleanAllOnStartup()).thenReturn(0);

        assertThatCode(() -> listener.onReady()).doesNotThrowAnyException();

        verify(cleanupService).cleanAllOnStartup();
    }

    @Test
    @DisplayName("On ready does not break application startup when the cleanup fails")
    void onReady_Success_FailureIsSwallowed() {
        when(cleanupService.cleanAllOnStartup()).thenThrow(new IllegalStateException("db down"));

        assertThatCode(() -> listener.onReady()).doesNotThrowAnyException();

        verify(cleanupService).cleanAllOnStartup();
    }
}
