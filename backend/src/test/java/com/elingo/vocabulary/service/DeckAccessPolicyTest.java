package com.elingo.vocabulary.service;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.premium.service.PremiumAccessService;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.support.TestDeckAccessView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class DeckAccessPolicyTest {

    @Mock
    private PremiumAccessService premiumAccessService;

    @InjectMocks
    private DeckAccessPolicy deckAccessPolicy;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    private DeckRepository.DeckAccessView deckView(DeckStatus status, OwnerType ownerType,
                                                   Long ownerId, boolean isPremium) {
        return TestDeckAccessView.of(1L, status, ownerType, ownerId, isPremium);
    }

    // ─────────────────────── isOwner ───────────────────────

    @Nested
    @DisplayName("isOwner")
    class IsOwnerTests {

        @Test
        @DisplayName("Returns true when the deck belongs to the user")
        void isOwner_True() {
            assertThat(deckAccessPolicy.isOwner(
                    deckView(DeckStatus.DRAFT, OwnerType.USER, USER_ID, false), USER_ID)).isTrue();
        }

        @Test
        @DisplayName("Returns false when the deck belongs to another user")
        void isOwner_False_OtherUser() {
            assertThat(deckAccessPolicy.isOwner(
                    deckView(DeckStatus.PUBLISHED, OwnerType.USER, OTHER_USER_ID, false), USER_ID)).isFalse();
        }

        @Test
        @DisplayName("Returns false for system decks even when the ids match")
        void isOwner_False_SystemDeck() {
            assertThat(deckAccessPolicy.isOwner(
                    deckView(DeckStatus.PUBLISHED, OwnerType.SYSTEM, USER_ID, false), USER_ID)).isFalse();
        }

        @Test
        @DisplayName("Returns false for anonymous users")
        void isOwner_False_Anonymous() {
            assertThat(deckAccessPolicy.isOwner(
                    deckView(DeckStatus.PUBLISHED, OwnerType.USER, USER_ID, false), null)).isFalse();
        }
    }

    // ─────────────────────── assertCanView ───────────────────────

    @Nested
    @DisplayName("assertCanView")
    class AssertCanViewTests {

        @Test
        @DisplayName("Allows any user to view a published deck")
        void assertCanView_Published() {
            assertThatCode(() -> deckAccessPolicy.assertCanView(
                    deckView(DeckStatus.PUBLISHED, OwnerType.SYSTEM, null, false), USER_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Allows an anonymous user to view a published deck")
        void assertCanView_Published_Anonymous() {
            assertThatCode(() -> deckAccessPolicy.assertCanView(
                    deckView(DeckStatus.PUBLISHED, OwnerType.SYSTEM, null, false), null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Allows the owner to view their own draft deck")
        void assertCanView_Draft_Owner() {
            assertThatCode(() -> deckAccessPolicy.assertCanView(
                    deckView(DeckStatus.DRAFT, OwnerType.USER, USER_ID, false), USER_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Rejects a draft deck of another user as not found")
        void assertCanView_Draft_OtherUser() {
            assertThatThrownBy(() -> deckAccessPolicy.assertCanView(
                    deckView(DeckStatus.DRAFT, OwnerType.USER, OTHER_USER_ID, false), USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_NOT_FOUND));
        }

        @Test
        @DisplayName("Rejects an archived deck for anonymous users")
        void assertCanView_Archived_Anonymous() {
            assertThatThrownBy(() -> deckAccessPolicy.assertCanView(
                    deckView(DeckStatus.ARCHIVED, OwnerType.SYSTEM, null, false), null))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_NOT_FOUND));
        }
    }

    // ─────────────────────── assertCanEdit ───────────────────────

    @Nested
    @DisplayName("assertCanEdit")
    class AssertCanEditTests {

        @Test
        @DisplayName("Allows the owner to edit their published deck")
        void assertCanEdit_Owner() {
            assertThatCode(() -> deckAccessPolicy.assertCanEdit(
                    deckView(DeckStatus.PUBLISHED, OwnerType.USER, USER_ID, false), USER_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Rejects a system deck with access denied")
        void assertCanEdit_Fail_SystemDeck() {
            assertThatThrownBy(() -> deckAccessPolicy.assertCanEdit(
                    deckView(DeckStatus.PUBLISHED, OwnerType.SYSTEM, null, false), USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_ACCESS_DENIED));
        }

        @Test
        @DisplayName("Rejects another user's published deck with access denied")
        void assertCanEdit_Fail_OtherUser() {
            assertThatThrownBy(() -> deckAccessPolicy.assertCanEdit(
                    deckView(DeckStatus.PUBLISHED, OwnerType.USER, OTHER_USER_ID, false), USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_ACCESS_DENIED));
        }

        @Test
        @DisplayName("Hides another user's draft deck behind not found")
        void assertCanEdit_Fail_DraftOfOtherUser() {
            assertThatThrownBy(() -> deckAccessPolicy.assertCanEdit(
                    deckView(DeckStatus.DRAFT, OwnerType.USER, OTHER_USER_ID, false), USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_NOT_FOUND));
        }

        @Test
        @DisplayName("Rejects anonymous users")
        void assertCanEdit_Fail_Anonymous() {
            assertThatThrownBy(() -> deckAccessPolicy.assertCanEdit(
                    deckView(DeckStatus.PUBLISHED, OwnerType.USER, USER_ID, false), null))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_ACCESS_DENIED));
        }
    }

    // ─────────────────────── assertCanLearn ───────────────────────

    @Nested
    @DisplayName("assertCanLearn")
    class AssertCanLearnTests {

        @Test
        @DisplayName("Allows learning a free published deck without premium check")
        void assertCanLearn_FreeDeck() {
            assertThatCode(() -> deckAccessPolicy.assertCanLearn(
                    deckView(DeckStatus.PUBLISHED, OwnerType.SYSTEM, null, false), USER_ID))
                    .doesNotThrowAnyException();

            verify(premiumAccessService, never()).requirePremium(USER_ID);
        }

        @Test
        @DisplayName("Allows learning a premium deck when the subscription is active")
        void assertCanLearn_PremiumDeck_ActiveSubscription() {
            assertThatCode(() -> deckAccessPolicy.assertCanLearn(
                    deckView(DeckStatus.PUBLISHED, OwnerType.SYSTEM, null, true), USER_ID))
                    .doesNotThrowAnyException();

            verify(premiumAccessService).requirePremium(USER_ID);
        }

        @Test
        @DisplayName("Rejects a premium deck when the subscription is missing")
        void assertCanLearn_Fail_PremiumRequired() {
            doThrow(new AppException(AppError.PREMIUM_REQUIRED))
                    .when(premiumAccessService).requirePremium(USER_ID);

            assertThatThrownBy(() -> deckAccessPolicy.assertCanLearn(
                    deckView(DeckStatus.PUBLISHED, OwnerType.SYSTEM, null, true), USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.PREMIUM_REQUIRED));
        }

        @Test
        @DisplayName("Rejects an inaccessible deck before checking premium")
        void assertCanLearn_Fail_DeckNotVisible() {
            assertThatThrownBy(() -> deckAccessPolicy.assertCanLearn(
                    deckView(DeckStatus.DRAFT, OwnerType.USER, OTHER_USER_ID, true), USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.DECK_NOT_FOUND));

            verify(premiumAccessService, never()).requirePremium(USER_ID);
        }
    }
}
