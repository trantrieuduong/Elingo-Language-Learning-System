package com.elingo.speaking.state;

import com.elingo.speaking.entity.ParticipantRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Tag("unit")
class LiveSpeakingRoomTest {

    private static final String ROOM_CODE = "ABC234";
    private static final Long ROOM_ID = 1L;
    private static final Long HOST_ID = 10L;
    private static final Long GUEST_ID = 20L;

    private LiveSpeakingRoom room;

    @BeforeEach
    void setUp() {
        room = new LiveSpeakingRoom(ROOM_CODE, ROOM_ID, HOST_ID, "Travel", (short) 4, false, 1000L);
    }

    private LivePeer peer(Long userId, ParticipantRole role) {
        return new LivePeer(userId, "User " + userId, null, role);
    }

    // ========================================================================
    // constructor
    // ========================================================================
    @Nested
    @DisplayName("constructor")
    class ConstructorTests {

        @Test
        @DisplayName("Room starts with the given settings and no peers")
        void constructor_Success() {
            assertThat(room.getRoomCode()).isEqualTo(ROOM_CODE);
            assertThat(room.getRoomId()).isEqualTo(ROOM_ID);
            assertThat(room.getHostId()).isEqualTo(HOST_ID);
            assertThat(room.getTopic()).isEqualTo("Travel");
            assertThat(room.getMaxParticipants()).isEqualTo((short) 4);
            assertThat(room.isPrivate()).isFalse();
            assertThat(room.getStartedAtMs()).isEqualTo(1000L);
            assertThat(room.isEmpty()).isTrue();
            assertThat(room.getPeers()).isEmpty();
        }
    }

    // ========================================================================
    // peers
    // ========================================================================
    @Nested
    @DisplayName("peers")
    class PeerTests {

        @Test
        @DisplayName("Put peer makes it retrievable and the room non-empty")
        void putPeer_Success() {
            LivePeer host = peer(HOST_ID, ParticipantRole.HOST);

            room.putPeer(host);

            assertThat(room.getPeer(HOST_ID)).isSameAs(host);
            assertThat(room.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("Put peer with an existing user id replaces the previous peer")
        void putPeer_Replaces() {
            LivePeer first = peer(GUEST_ID, ParticipantRole.GUEST);
            LivePeer second = peer(GUEST_ID, ParticipantRole.GUEST);
            room.putPeer(first);

            room.putPeer(second);

            assertThat(room.getPeers()).hasSize(1);
            assertThat(room.getPeer(GUEST_ID)).isSameAs(second);
        }

        @Test
        @DisplayName("Get peer returns null for an unknown user")
        void getPeer_Unknown() {
            assertThat(room.getPeer(999L)).isNull();
        }

        @Test
        @DisplayName("Remove peer returns the removed peer and empties the room")
        void removePeer_Success() {
            LivePeer host = peer(HOST_ID, ParticipantRole.HOST);
            room.putPeer(host);

            LivePeer removed = room.removePeer(HOST_ID);

            assertThat(removed).isSameAs(host);
            assertThat(room.getPeer(HOST_ID)).isNull();
            assertThat(room.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("Remove peer returns null for an unknown user")
        void removePeer_Unknown() {
            assertThat(room.removePeer(999L)).isNull();
        }
    }

    // ========================================================================
    // online state
    // ========================================================================
    @Nested
    @DisplayName("online state")
    class OnlineTests {

        @Test
        @DisplayName("A newly added peer is online")
        void isOnline_NewPeer() {
            room.putPeer(peer(HOST_ID, ParticipantRole.HOST));

            assertThat(room.isOnline(HOST_ID)).isTrue();
        }

        @Test
        @DisplayName("An unknown user is not online")
        void isOnline_Unknown() {
            assertThat(room.isOnline(999L)).isFalse();
        }

        @Test
        @DisplayName("A peer that went offline is not online")
        void isOnline_Offline() {
            LivePeer host = peer(HOST_ID, ParticipantRole.HOST);
            room.putPeer(host);
            host.setOnline(false);

            assertThat(room.isOnline(HOST_ID)).isFalse();
        }

        @Test
        @DisplayName("Online peers keep the join order and skip offline peers")
        void onlinePeers_FiltersOffline() {
            LivePeer host = peer(HOST_ID, ParticipantRole.HOST);
            LivePeer guest = peer(GUEST_ID, ParticipantRole.GUEST);
            LivePeer third = peer(30L, ParticipantRole.GUEST);
            room.putPeer(host);
            room.putPeer(guest);
            room.putPeer(third);
            guest.setOnline(false);

            List<LivePeer> online = room.onlinePeers();

            assertThat(online).containsExactly(host, third);
        }

        @Test
        @DisplayName("Online peers is empty when nobody is online")
        void onlinePeers_Empty() {
            assertThat(room.onlinePeers()).isEmpty();
        }
    }

    // ========================================================================
    // grace
    // ========================================================================
    @Nested
    @DisplayName("grace futures")
    class GraceTests {

        @Test
        @DisplayName("Put grace stores the future so it can be cancelled later")
        void putGrace_Success() {
            ScheduledFuture<?> future = mock(ScheduledFuture.class);

            room.putGrace(GUEST_ID, future);

            assertThat(room.getGraceFutures()).containsEntry(GUEST_ID, future);
            verify(future, never()).cancel(false);
        }

        @Test
        @DisplayName("Put grace cancels the previous future of the same user")
        void putGrace_CancelsPrevious() {
            ScheduledFuture<?> previous = mock(ScheduledFuture.class);
            ScheduledFuture<?> current = mock(ScheduledFuture.class);
            room.putGrace(GUEST_ID, previous);

            room.putGrace(GUEST_ID, current);

            verify(previous).cancel(false);
            verify(current, never()).cancel(false);
            assertThat(room.getGraceFutures()).containsEntry(GUEST_ID, current).hasSize(1);
        }

        @Test
        @DisplayName("Cancel grace cancels and removes the future, returning true")
        void cancelGrace_Success() {
            ScheduledFuture<?> future = mock(ScheduledFuture.class);
            room.putGrace(GUEST_ID, future);

            boolean cancelled = room.cancelGrace(GUEST_ID);

            assertThat(cancelled).isTrue();
            verify(future).cancel(false);
            assertThat(room.getGraceFutures()).isEmpty();
        }

        @Test
        @DisplayName("Cancel grace returns false when the user has no future")
        void cancelGrace_NoFuture() {
            assertThat(room.cancelGrace(GUEST_ID)).isFalse();
        }

        @Test
        @DisplayName("Cancel grace twice only succeeds the first time")
        void cancelGrace_Twice() {
            room.putGrace(GUEST_ID, mock(ScheduledFuture.class));

            assertThat(room.cancelGrace(GUEST_ID)).isTrue();
            assertThat(room.cancelGrace(GUEST_ID)).isFalse();
        }

        @Test
        @DisplayName("Cancel all graces cancels every future and clears the map")
        void cancelAllGraces_Success() {
            ScheduledFuture<?> first = mock(ScheduledFuture.class);
            ScheduledFuture<?> second = mock(ScheduledFuture.class);
            room.putGrace(HOST_ID, first);
            room.putGrace(GUEST_ID, second);

            room.cancelAllGraces();

            verify(first).cancel(false);
            verify(second).cancel(false);
            assertThat(room.getGraceFutures()).isEmpty();
        }

        @Test
        @DisplayName("Cancel all graces does nothing when there are no futures")
        void cancelAllGraces_Empty() {
            room.cancelAllGraces();

            assertThat(room.getGraceFutures()).isEmpty();
        }
    }

    // ========================================================================
    // lock
    // ========================================================================
    @Nested
    @DisplayName("lock")
    class LockTests {

        @Test
        @DisplayName("Lock and unlock toggle the lock held by the current thread")
        void lock_Success() {
            room.lock();
            assertThat(room.getLock().isHeldByCurrentThread()).isTrue();

            room.unlock();
            assertThat(room.getLock().isLocked()).isFalse();
        }

        @Test
        @DisplayName("Lock is reentrant for the same thread")
        void lock_Reentrant() {
            room.lock();
            room.lock();
            assertThat(room.getLock().getHoldCount()).isEqualTo(2);

            room.unlock();
            room.unlock();
            assertThat(room.getLock().isLocked()).isFalse();
        }
    }
}
