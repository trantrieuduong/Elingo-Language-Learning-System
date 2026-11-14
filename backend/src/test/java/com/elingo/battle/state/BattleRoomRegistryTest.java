package com.elingo.battle.state;

import com.elingo.battle.entity.BattleMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class BattleRoomRegistryTest {

    private static final Long HOST_A = 1L;
    private static final Long HOST_B = 2L;

    private BattleRoomRegistry registry;
    private LiveRoom roomA;
    private LiveRoom roomB;

    @BeforeEach
    void setUp() {
        registry = new BattleRoomRegistry();
        roomA = new LiveRoom("ABC234", HOST_A, BattleMode.MULTIPLE_CHOICE);
        roomB = new LiveRoom("XYZ789", HOST_B, BattleMode.TYPING);
    }

    // ========================================================================
    // register and lookup
    // ========================================================================
    @Nested
    @DisplayName("register and lookup")
    class RegisterTests {

        @Test
        @DisplayName("Registered room can be found by code and its host is marked as hosting")
        void register_Success() {
            registry.register(roomA);

            assertThat(registry.getByCode("ABC234")).contains(roomA);
            assertThat(registry.codeExists("ABC234")).isTrue();
            assertThat(registry.isHosting(HOST_A)).isTrue();
        }

        @Test
        @DisplayName("Lookup returns empty results for unknown code and non-host")
        void lookup_Unknown() {
            registry.register(roomA);

            assertThat(registry.getByCode("NOPE12")).isEmpty();
            assertThat(registry.codeExists("NOPE12")).isFalse();
            assertThat(registry.isHosting(HOST_B)).isFalse();
        }

        @Test
        @DisplayName("Several rooms can be registered at the same time")
        void register_MultipleRooms() {
            registry.register(roomA);
            registry.register(roomB);

            assertThat(registry.getByCode("ABC234")).contains(roomA);
            assertThat(registry.getByCode("XYZ789")).contains(roomB);
            assertThat(registry.isHosting(HOST_A)).isTrue();
            assertThat(registry.isHosting(HOST_B)).isTrue();
        }
    }

    // ========================================================================
    // removeByHost
    // ========================================================================
    @Nested
    @DisplayName("removeByHost")
    class RemoveByHostTests {

        @Test
        @DisplayName("Remove by host returns and deletes the hosted room")
        void removeByHost_Success() {
            registry.register(roomA);

            assertThat(registry.removeByHost(HOST_A)).contains(roomA);

            assertThat(registry.getByCode("ABC234")).isEmpty();
            assertThat(registry.codeExists("ABC234")).isFalse();
            assertThat(registry.isHosting(HOST_A)).isFalse();
        }

        @Test
        @DisplayName("Remove by host returns empty when the user is not hosting")
        void removeByHost_NotHosting() {
            registry.register(roomA);

            assertThat(registry.removeByHost(HOST_B)).isEmpty();
            assertThat(registry.getByCode("ABC234")).contains(roomA);
        }

        @Test
        @DisplayName("Remove by host only removes the room of that host")
        void removeByHost_KeepsOtherRooms() {
            registry.register(roomA);
            registry.register(roomB);

            registry.removeByHost(HOST_A);

            assertThat(registry.getByCode("XYZ789")).contains(roomB);
            assertThat(registry.isHosting(HOST_B)).isTrue();
        }

        @Test
        @DisplayName("Remove by host twice returns empty the second time")
        void removeByHost_Twice() {
            registry.register(roomA);
            registry.removeByHost(HOST_A);

            assertThat(registry.removeByHost(HOST_A)).isEmpty();
        }
    }

    // ========================================================================
    // removeByCode
    // ========================================================================
    @Nested
    @DisplayName("removeByCode")
    class RemoveByCodeTests {

        @Test
        @DisplayName("Remove by code returns and deletes the room and clears the host mapping")
        void removeByCode_Success() {
            registry.register(roomA);

            assertThat(registry.removeByCode("ABC234")).contains(roomA);

            assertThat(registry.codeExists("ABC234")).isFalse();
            assertThat(registry.isHosting(HOST_A)).isFalse();
        }

        @Test
        @DisplayName("Remove by code returns empty for an unknown code")
        void removeByCode_Unknown() {
            registry.register(roomA);

            assertThat(registry.removeByCode("NOPE12")).isEmpty();
            assertThat(registry.isHosting(HOST_A)).isTrue();
        }

        @Test
        @DisplayName("Remove by code only removes that room")
        void removeByCode_KeepsOtherRooms() {
            registry.register(roomA);
            registry.register(roomB);

            registry.removeByCode("ABC234");

            assertThat(registry.getByCode("XYZ789")).contains(roomB);
            assertThat(registry.isHosting(HOST_B)).isTrue();
        }
    }
}
