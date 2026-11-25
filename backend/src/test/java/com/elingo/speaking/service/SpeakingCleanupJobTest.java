package com.elingo.speaking.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SpeakingCleanupJobTest {

    @Mock
    private SpeakingCleanupService cleanupService;

    @InjectMocks
    private SpeakingCleanupJob job;

    @Test
    @DisplayName("Run closes the stale rooms")
    void run_Success() {
        when(cleanupService.cleanStaleRooms()).thenReturn(2);

        assertThatCode(() -> job.run()).doesNotThrowAnyException();

        verify(cleanupService).cleanStaleRooms();
    }

    @Test
    @DisplayName("Run succeeds when there are no stale rooms")
    void run_Success_NothingToClose() {
        when(cleanupService.cleanStaleRooms()).thenReturn(0);

        assertThatCode(() -> job.run()).doesNotThrowAnyException();

        verify(cleanupService).cleanStaleRooms();
    }

    @Test
    @DisplayName("Run swallows a failure so the scheduled job keeps running")
    void run_Success_FailureIsSwallowed() {
        when(cleanupService.cleanStaleRooms()).thenThrow(new IllegalStateException("db down"));

        assertThatCode(() -> job.run()).doesNotThrowAnyException();

        verify(cleanupService).cleanStaleRooms();
    }

    @Test
    @DisplayName("Run can be triggered repeatedly after a failure")
    void run_Success_RunsAgainAfterFailure() {
        when(cleanupService.cleanStaleRooms())
                .thenThrow(new IllegalStateException("db down"))
                .thenReturn(1);

        job.run();
        job.run();

        verify(cleanupService, times(2)).cleanStaleRooms();
    }
}
