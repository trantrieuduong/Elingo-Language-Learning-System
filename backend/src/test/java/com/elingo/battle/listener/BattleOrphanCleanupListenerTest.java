package com.elingo.battle.listener;

import com.elingo.battle.service.BattleMatchPersistenceService;
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
class BattleOrphanCleanupListenerTest {

    @Mock
    private BattleMatchPersistenceService persistence;

    @InjectMocks
    private BattleOrphanCleanupListener listener;

    @Test
    @DisplayName("Cancel orphans cancels the unfinished matches left from the previous run")
    void cancelOrphans_Success() {
        when(persistence.cancelOrphanMatches()).thenReturn(3);

        assertThatCode(() -> listener.cancelOrphans()).doesNotThrowAnyException();

        verify(persistence).cancelOrphanMatches();
    }

    @Test
    @DisplayName("Cancel orphans succeeds when there is nothing to cancel")
    void cancelOrphans_Success_NothingToCancel() {
        when(persistence.cancelOrphanMatches()).thenReturn(0);

        assertThatCode(() -> listener.cancelOrphans()).doesNotThrowAnyException();

        verify(persistence).cancelOrphanMatches();
    }

    @Test
    @DisplayName("Cancel orphans does not break application startup when the cleanup fails")
    void cancelOrphans_Success_FailureIsSwallowed() {
        when(persistence.cancelOrphanMatches()).thenThrow(new IllegalStateException("db down"));

        assertThatCode(() -> listener.cancelOrphans()).doesNotThrowAnyException();

        verify(persistence).cancelOrphanMatches();
    }
}
