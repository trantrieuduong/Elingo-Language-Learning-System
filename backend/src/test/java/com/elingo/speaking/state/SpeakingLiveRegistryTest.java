package com.elingo.speaking.state;

import com.elingo.speaking.entity.ParticipantRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class SpeakingLiveRegistryTest {

    private static final String CODE_A = "AAAAAA";
    private static final String CODE_B = "BBBBBB";
    private static final Long USER_1 = 1L;
    private static final Long USER_2 = 2L;
    private static final Long USER_3 = 3L;

    private SpeakingLiveRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SpeakingLiveRegistry();
    }

    private LiveSpeakingRoom room(String code) {
        return new LiveSpeakingRoom(code, 1L, USER_1, "Topic", (short) 4, false, 1000L);
    }

    private LivePeer peer(Long userId) {
        return new LivePeer(userId, "User " + userId, null, ParticipantRole.GUEST);
    }

    // ========================================================================
    // getOrCreate
    // ========================================================================
    @Nested
    @DisplayName("getOrCreate")
    class GetOrCreateTests {

        @Test
        @DisplayName("Get or create builds the room with the factory when it does not exist")
        void getOrCreate_Creates() {
            LiveSpeakingRoom created = room(CODE_A);

            LiveSpeakingRoom result = registry.getOrCreate(CODE_A, () -> created);

            assertThat(result).isSameAs(created);
            assertThat(registry.get(CODE_A)).isSameAs(created);
        }

        @Test
        @DisplayName("Get or create returns the existing room without calling the factory again")
        void getOrCreate_ReturnsExisting() {
            LiveSpeakingRoom existing = registry.getOrCreate(CODE_A, () -> room(CODE_A));
            AtomicInteger factoryCalls = new AtomicInteger();

            LiveSpeakingRoom result = registry.getOrCreate(CODE_A, () -> {
                factoryCalls.incrementAndGet();
                return room(CODE_A);
            });

            assertThat(result).isSameAs(existing);
            assertThat(factoryCalls).hasValue(0);
        }
    }

    // ========================================================================
    // get
    // ========================================================================
    @Nested
    @DisplayName("get")
    class GetTests {

        @Test
        @DisplayName("Get returns null for an unknown code")
        void get_Unknown() {
            assertThat(registry.get(CODE_A)).isNull();
        }
    }

    // ========================================================================
    // getByUser / bindUser / unbindUser
    // ========================================================================
    @Nested
    @DisplayName("user binding")
    class UserBindingTests {

        @Test
        @DisplayName("Get by user returns the room the user is bound to")
        void getByUser_Bound() {
            LiveSpeakingRoom roomA = registry.getOrCreate(CODE_A, () -> room(CODE_A));
            registry.bindUser(USER_1, CODE_A);

            assertThat(registry.getByUser(USER_1)).isSameAs(roomA);
        }

        @Test
        @DisplayName("Get by user returns null when the user is not bound")
        void getByUser_NotBound() {
            registry.getOrCreate(CODE_A, () -> room(CODE_A));

            assertThat(registry.getByUser(USER_1)).isNull();
        }

        @Test
        @DisplayName("Get by user returns null when the bound room no longer exists")
        void getByUser_RoomGone() {
            registry.bindUser(USER_1, CODE_A);

            assertThat(registry.getByUser(USER_1)).isNull();
        }

        @Test
        @DisplayName("Binding a user again moves them to the new room")
        void bindUser_Rebind() {
            registry.getOrCreate(CODE_A, () -> room(CODE_A));
            LiveSpeakingRoom roomB = registry.getOrCreate(CODE_B, () -> room(CODE_B));
            registry.bindUser(USER_1, CODE_A);

            registry.bindUser(USER_1, CODE_B);

            assertThat(registry.getByUser(USER_1)).isSameAs(roomB);
        }

        @Test
        @DisplayName("Unbind user removes the binding when the code matches")
        void unbindUser_Match() {
            registry.getOrCreate(CODE_A, () -> room(CODE_A));
            registry.bindUser(USER_1, CODE_A);

            registry.unbindUser(USER_1, CODE_A);

            assertThat(registry.getByUser(USER_1)).isNull();
        }

        @Test
        @DisplayName("Unbind user keeps the binding when the user is bound to another room")
        void unbindUser_OtherRoom() {
            LiveSpeakingRoom roomB = registry.getOrCreate(CODE_B, () -> room(CODE_B));
            registry.bindUser(USER_1, CODE_B);

            registry.unbindUser(USER_1, CODE_A);

            assertThat(registry.getByUser(USER_1)).isSameAs(roomB);
        }

        @Test
        @DisplayName("Unbind user does nothing for an unbound user")
        void unbindUser_NotBound() {
            registry.unbindUser(USER_1, CODE_A);

            assertThat(registry.getByUser(USER_1)).isNull();
        }
    }

    // ========================================================================
    // removeRoom
    // ========================================================================
    @Nested
    @DisplayName("removeRoom")
    class RemoveRoomTests {

        @Test
        @DisplayName("Remove room deletes the room and unbinds all of its peers")
        void removeRoom_Success() {
            LiveSpeakingRoom roomA = registry.getOrCreate(CODE_A, () -> room(CODE_A));
            roomA.putPeer(peer(USER_1));
            roomA.putPeer(peer(USER_2));
            registry.bindUser(USER_1, CODE_A);
            registry.bindUser(USER_2, CODE_A);

            registry.removeRoom(roomA);

            assertThat(registry.get(CODE_A)).isNull();
            assertThat(registry.getByUser(USER_1)).isNull();
            assertThat(registry.getByUser(USER_2)).isNull();
        }

        @Test
        @DisplayName("Remove room does not unbind a peer that already moved to another room")
        void removeRoom_PeerMovedElsewhere() {
            LiveSpeakingRoom roomA = registry.getOrCreate(CODE_A, () -> room(CODE_A));
            LiveSpeakingRoom roomB = registry.getOrCreate(CODE_B, () -> room(CODE_B));
            roomA.putPeer(peer(USER_1));
            registry.bindUser(USER_1, CODE_B);

            registry.removeRoom(roomA);

            assertThat(registry.getByUser(USER_1)).isSameAs(roomB);
        }

        @Test
        @DisplayName("Remove room leaves other rooms untouched")
        void removeRoom_OtherRoomsUntouched() {
            LiveSpeakingRoom roomA = registry.getOrCreate(CODE_A, () -> room(CODE_A));
            LiveSpeakingRoom roomB = registry.getOrCreate(CODE_B, () -> room(CODE_B));
            roomB.putPeer(peer(USER_3));
            registry.bindUser(USER_3, CODE_B);

            registry.removeRoom(roomA);

            assertThat(registry.get(CODE_B)).isSameAs(roomB);
            assertThat(registry.getByUser(USER_3)).isSameAs(roomB);
        }

        @Test
        @DisplayName("Remove room works for a room without peers")
        void removeRoom_NoPeers() {
            LiveSpeakingRoom roomA = registry.getOrCreate(CODE_A, () -> room(CODE_A));

            registry.removeRoom(roomA);

            assertThat(registry.get(CODE_A)).isNull();
        }
    }
}
