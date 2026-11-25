package com.elingo.speaking.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class SpeakingSessionRegistryTest {

    private static final Long USER_A = 1L;
    private static final Long USER_B = 2L;

    private SpeakingSessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SpeakingSessionRegistry();
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
        @DisplayName("User stays online while the same session still has another subscription")
        void removeSubscription_SessionHasAnotherSub() {
            registry.addSubscription(USER_A, "s1", "sub-0");
            registry.addSubscription(USER_A, "s1", "sub-1");

            assertThat(registry.removeSubscription("s1", "sub-0")).isNull();
            assertThat(registry.removeSubscription("s1", "sub-1")).isEqualTo(USER_A);
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
        @DisplayName("A missing subscription id is stored under the default key")
        void removeSubscription_NullSubscriptionId() {
            registry.addSubscription(USER_A, "s1", null);

            assertThat(registry.removeSubscription("s1", null)).isEqualTo(USER_A);
        }

        @Test
        @DisplayName("A null subscription id and the literal default id refer to the same subscription")
        void removeSubscription_DefaultKeyEquivalence() {
            registry.addSubscription(USER_A, "s1", null);

            assertThat(registry.removeSubscription("s1", "default")).isEqualTo(USER_A);
        }

        @Test
        @DisplayName("Users are tracked independently")
        void removeSubscription_UsersIndependent() {
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
        @DisplayName("Removing the only session of the user returns true")
        void removeAndCheckLast_LastSession() {
            registry.addSubscription(USER_A, "s1", "sub-0");

            assertThat(registry.removeAndCheckLast(USER_A, "s1")).isTrue();
        }

        @Test
        @DisplayName("Removing one of two sessions returns false")
        void removeAndCheckLast_OtherSessionRemains() {
            registry.addSubscription(USER_A, "s1", "sub-0");
            registry.addSubscription(USER_A, "s2", "sub-0");

            assertThat(registry.removeAndCheckLast(USER_A, "s1")).isFalse();
            assertThat(registry.removeAndCheckLast(USER_A, "s2")).isTrue();
        }

        @Test
        @DisplayName("A user without any session is treated as already offline")
        void removeAndCheckLast_UnknownUser() {
            assertThat(registry.removeAndCheckLast(USER_A, "s1")).isTrue();
        }

        @Test
        @DisplayName("An unknown session of a user that has another session returns false")
        void removeAndCheckLast_UnknownSession() {
            registry.addSubscription(USER_A, "s1", "sub-0");

            assertThat(registry.removeAndCheckLast(USER_A, "other")).isFalse();
        }

        @Test
        @DisplayName("Removing a session also drops all of its subscriptions")
        void removeAndCheckLast_DropsSubscriptions() {
            registry.addSubscription(USER_A, "s1", "sub-0");
            registry.addSubscription(USER_A, "s1", "sub-1");
            registry.removeAndCheckLast(USER_A, "s1");

            assertThat(registry.removeSubscription("s1", "sub-0")).isNull();
            assertThat(registry.removeSubscription("s1", "sub-1")).isNull();
        }

        @Test
        @DisplayName("Removing a session does not touch another session whose id starts the same")
        void removeAndCheckLast_SessionIdPrefix() {
            registry.addSubscription(USER_A, "s1", "sub-0");
            registry.addSubscription(USER_A, "s10", "sub-0");

            assertThat(registry.removeAndCheckLast(USER_A, "s1")).isFalse();
            assertThat(registry.removeSubscription("s10", "sub-0")).isEqualTo(USER_A);
        }

        @Test
        @DisplayName("The user can subscribe again after going offline")
        void removeAndCheckLast_ResubscribeAfterOffline() {
            registry.addSubscription(USER_A, "s1", "sub-0");
            registry.removeAndCheckLast(USER_A, "s1");

            registry.addSubscription(USER_A, "s2", "sub-0");

            assertThat(registry.removeSubscription("s2", "sub-0")).isEqualTo(USER_A);
        }
    }
}
