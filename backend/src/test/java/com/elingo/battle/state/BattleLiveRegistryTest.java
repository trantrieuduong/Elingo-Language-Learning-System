package com.elingo.battle.state;

import com.elingo.battle.entity.BattleMatchStatus;
import com.elingo.battle.entity.BattleMatchType;
import com.elingo.battle.entity.BattleMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class BattleLiveRegistryTest {

    private static final Long USER_A = 1L;
    private static final Long USER_B = 2L;
    private static final Long USER_C = 3L;
    private static final Long USER_D = 4L;

    private BattleLiveRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new BattleLiveRegistry();
    }

    private LiveMatch buildMatch(Long matchId, Long userA, Long userB) {
        return new LiveMatch(matchId, BattleMode.MULTIPLE_CHOICE, BattleMatchType.RANDOM, 10,
                userA, userB, Map.of(userA, 11L, userB, 12L), List.of());
    }

    // ========================================================================
    // register / get
    // ========================================================================
    @Nested
    @DisplayName("register and lookup")
    class RegisterTests {

        @Test
        @DisplayName("Registered match can be found by match id and by either user")
        void register_Success() {
            LiveMatch match = buildMatch(100L, USER_A, USER_B);

            registry.register(match);

            assertThat(registry.get(100L)).isSameAs(match);
            assertThat(registry.getByUser(USER_A)).isSameAs(match);
            assertThat(registry.getByUser(USER_B)).isSameAs(match);
        }

        @Test
        @DisplayName("Lookup returns null for unknown match and unknown user")
        void lookup_Unknown() {
            registry.register(buildMatch(100L, USER_A, USER_B));

            assertThat(registry.get(999L)).isNull();
            assertThat(registry.getByUser(USER_C)).isNull();
        }

        @Test
        @DisplayName("Several matches can be registered at the same time")
        void register_MultipleMatches() {
            LiveMatch first = buildMatch(100L, USER_A, USER_B);
            LiveMatch second = buildMatch(101L, USER_C, USER_D);

            registry.register(first);
            registry.register(second);

            assertThat(registry.getByUser(USER_A)).isSameAs(first);
            assertThat(registry.getByUser(USER_D)).isSameAs(second);
        }
    }

    // ========================================================================
    // isUserInActiveMatch
    // ========================================================================
    @Nested
    @DisplayName("isUserInActiveMatch")
    class IsUserInActiveMatchTests {

        @Test
        @DisplayName("User is in an active match while the match is in progress")
        void isUserInActiveMatch_InProgress() {
            registry.register(buildMatch(100L, USER_A, USER_B));

            assertThat(registry.isUserInActiveMatch(USER_A)).isTrue();
            assertThat(registry.isUserInActiveMatch(USER_B)).isTrue();
        }

        @Test
        @DisplayName("User is not in an active match when they have no match")
        void isUserInActiveMatch_NoMatch() {
            assertThat(registry.isUserInActiveMatch(USER_A)).isFalse();
        }

        @Test
        @DisplayName("User is not in an active match when the match is finished or cancelled")
        void isUserInActiveMatch_NotInProgress() {
            LiveMatch match = buildMatch(100L, USER_A, USER_B);
            registry.register(match);

            match.setStatus(BattleMatchStatus.FINISHED);
            assertThat(registry.isUserInActiveMatch(USER_A)).isFalse();

            match.setStatus(BattleMatchStatus.CANCELLED);
            assertThat(registry.isUserInActiveMatch(USER_A)).isFalse();
        }
    }

    // ========================================================================
    // remove
    // ========================================================================
    @Nested
    @DisplayName("remove")
    class RemoveTests {

        @Test
        @DisplayName("Remove deletes the match and both user mappings")
        void remove_Success() {
            LiveMatch match = buildMatch(100L, USER_A, USER_B);
            registry.register(match);

            registry.remove(match);

            assertThat(registry.get(100L)).isNull();
            assertThat(registry.getByUser(USER_A)).isNull();
            assertThat(registry.getByUser(USER_B)).isNull();
            assertThat(registry.isUserInActiveMatch(USER_A)).isFalse();
        }

        @Test
        @DisplayName("Remove does not affect other matches")
        void remove_KeepsOtherMatches() {
            LiveMatch first = buildMatch(100L, USER_A, USER_B);
            LiveMatch second = buildMatch(101L, USER_C, USER_D);
            registry.register(first);
            registry.register(second);

            registry.remove(first);

            assertThat(registry.getByUser(USER_C)).isSameAs(second);
            assertThat(registry.get(101L)).isSameAs(second);
        }

        @Test
        @DisplayName("Remove does not unmap a user who has already joined a newer match")
        void remove_KeepsMappingOfNewerMatch() {
            LiveMatch oldMatch = buildMatch(100L, USER_A, USER_B);
            registry.register(oldMatch);
            LiveMatch newMatch = buildMatch(101L, USER_A, USER_C);
            registry.register(newMatch);

            registry.remove(oldMatch);

            assertThat(registry.getByUser(USER_A)).isSameAs(newMatch);
            assertThat(registry.getByUser(USER_C)).isSameAs(newMatch);
        }

        @Test
        @DisplayName("Remove can be called for a match that is not registered")
        void remove_NotRegistered() {
            registry.remove(buildMatch(100L, USER_A, USER_B));

            assertThat(registry.get(100L)).isNull();
        }
    }
}
