package com.elingo.battle.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class BattleSessionRegistryTest {

    private static final Long USER_A = 1L;
    private static final Long USER_B = 2L;

    private BattleSessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new BattleSessionRegistry();
    }

    // ========================================================================
    // removeSubscription
    // ========================================================================
    @Nested
    @DisplayName("removeSubscription")
    class RemoveSubscriptionTests {

        @Test
        @DisplayName("Removing the only subscription of the only session returns the user id")
        void removeSubscription_LastSession() {
            registry.addSubscription(USER_A, "s1", "sub-0");

            assertThat(registry.removeSubscription("s1", "sub-0")).isEqualTo(USER_A);
        }

        @Test
        @DisplayName("Removing an unknown subscription returns null")
        void removeSubscription_Unknown() {
            registry.addSubscription(USER_A, "s1", "sub-0");

            assertThat(registry.removeSubscription("s1", "sub-9")).isNull();
            assertThat(registry.removeSubscription("s2", "sub-0")).isNull();
        }

        @Test
        @DisplayName("Removing the same subscription twice returns null the second time")
        void removeSubscription_Twice() {
            registry.addSubscription(USER_A, "s1", "sub-0");
            registry.removeSubscription("s1", "sub-0");

            assertThat(registry.removeSubscription("s1", "sub-0")).isNull();
        }

        @Test
        @DisplayName("User stays online while another tab (session) is still subscribed")
        void removeSubscription_OtherSessionRemains() {
            registry.addSubscription(USER_A, "s1", "sub-0");
            registry.addSubscription(USER_A, "s2", "sub-0");

            assertThat(registry.removeSubscription("s1", "sub-0")).isNull();
            assertThat(registry.removeSubscription("s2", "sub-0")).isEqualTo(USER_A);
        }

        @Test
        @DisplayName("User stays online while the same session still has another subscription")
        void removeSubscription_SameSessionOtherSubscription() {
            registry.addSubscription(USER_A, "s1", "sub-0");
            registry.addSubscription(USER_A, "s1", "sub-1");

            assertThat(registry.removeSubscription("s1", "sub-0")).isNull();
            assertThat(registry.removeSubscription("s1", "sub-1")).isEqualTo(USER_A);
        }

        @Test
        @DisplayName("A null subscription id is stored under a default key")
        void removeSubscription_NullSubscriptionId() {
            registry.addSubscription(USER_A, "s1", null);

            assertThat(registry.removeSubscription("s1", null)).isEqualTo(USER_A);
        }

        @Test
        @DisplayName("Session ids that share a prefix are not mixed up")
        void removeSubscription_PrefixSessionIds() {
            registry.addSubscription(USER_A, "s1", "sub-0");
            registry.addSubscription(USER_A, "s10", "sub-0");

            // s10 vẫn còn subscription nên user chưa offline
            assertThat(registry.removeSubscription("s1", "sub-0")).isNull();
            assertThat(registry.removeSubscription("s10", "sub-0")).isEqualTo(USER_A);
        }

        @Test
        @DisplayName("Users are tracked independently")
        void removeSubscription_DifferentUsers() {
            registry.addSubscription(USER_A, "s1", "sub-0");
            registry.addSubscription(USER_B, "s2", "sub-0");

            assertThat(registry.removeSubscription("s1", "sub-0")).isEqualTo(USER_A);
            assertThat(registry.removeSubscription("s2", "sub-0")).isEqualTo(USER_B);
        }
    }

    // ========================================================================
    // removeAndCheckLast
    // ========================================================================
    @Nested
    @DisplayName("removeAndCheckLast")
    class RemoveAndCheckLastTests {

        @Test
        @DisplayName("Disconnecting the only session returns true")
        void removeAndCheckLast_OnlySession() {
            registry.addSubscription(USER_A, "s1", "sub-0");

            assertThat(registry.removeAndCheckLast(USER_A, "s1")).isTrue();
        }

        @Test
        @DisplayName("Disconnecting one of several sessions returns false until the last one closes")
        void removeAndCheckLast_SeveralSessions() {
            registry.addSubscription(USER_A, "s1", "sub-0");
            registry.addSubscription(USER_A, "s2", "sub-0");

            assertThat(registry.removeAndCheckLast(USER_A, "s1")).isFalse();
            assertThat(registry.removeAndCheckLast(USER_A, "s2")).isTrue();
        }

        @Test
        @DisplayName("Disconnecting a user without any tracked session returns true")
        void removeAndCheckLast_UnknownUser() {
            assertThat(registry.removeAndCheckLast(USER_A, "s1")).isTrue();
        }

        @Test
        @DisplayName("Disconnecting an unknown session of a tracked user returns false")
        void removeAndCheckLast_UnknownSession() {
            registry.addSubscription(USER_A, "s1", "sub-0");

            assertThat(registry.removeAndCheckLast(USER_A, "s-unknown")).isFalse();
            assertThat(registry.removeSubscription("s1", "sub-0")).isEqualTo(USER_A);
        }

        @Test
        @DisplayName("Disconnecting a session removes all its subscriptions")
        void removeAndCheckLast_RemovesSubscriptions() {
            registry.addSubscription(USER_A, "s1", "sub-0");
            registry.addSubscription(USER_A, "s1", "sub-1");

            registry.removeAndCheckLast(USER_A, "s1");

            assertThat(registry.removeSubscription("s1", "sub-0")).isNull();
            assertThat(registry.removeSubscription("s1", "sub-1")).isNull();
        }

        @Test
        @DisplayName("Disconnecting a session keeps the subscriptions of other sessions")
        void removeAndCheckLast_KeepsOtherSessions() {
            registry.addSubscription(USER_A, "s1", "sub-0");
            registry.addSubscription(USER_A, "s10", "sub-0");

            registry.removeAndCheckLast(USER_A, "s1");

            assertThat(registry.removeSubscription("s10", "sub-0")).isEqualTo(USER_A);
        }

        @Test
        @DisplayName("A user can subscribe again after going completely offline")
        void removeAndCheckLast_ReconnectAfterOffline() {
            registry.addSubscription(USER_A, "s1", "sub-0");
            registry.removeAndCheckLast(USER_A, "s1");

            registry.addSubscription(USER_A, "s2", "sub-0");

            assertThat(registry.removeSubscription("s2", "sub-0")).isEqualTo(USER_A);
        }
    }
}
