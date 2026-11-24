package com.elingo.admin.content.service;

import com.elingo.vocabulary.dto.request.AdminDeckSearchRequest;
import com.elingo.vocabulary.dto.request.SystemDeckRequest;
import com.elingo.vocabulary.dto.request.UpdateDeckStatusRequest;
import com.elingo.admin.content.service.impl.AdminDeckServiceImpl;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
import com.elingo.vocabulary.service.DeckService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AdminDeckServiceTest {

    @Mock
    private DeckService deckService;

    @InjectMocks
    private AdminDeckServiceImpl adminDeckService;

    private DeckResponse createSampleDeckResponse(Long id, String title, DeckStatus status) {
        return new DeckResponse(
                id,
                title,
                "slug-" + id,
                "Description",
                false,
                "cover.png",
                status,
                OwnerType.SYSTEM,
                null,
                0,
                0,
                0,
                LocalDateTime.now(),
                Set.of(),
                Set.of()
        );
    }

    @Nested
    @DisplayName("searchDecks")
    class SearchDecksTests {

        @Test
        @DisplayName("Search decks successfully delegating to deckService")
        void searchDecks_Success() {
            AdminDeckSearchRequest request = new AdminDeckSearchRequest("test", OwnerType.SYSTEM, DeckStatus.PUBLISHED, false, Set.of(), Set.of());
            Pageable pageable = PageRequest.of(0, 10);
            PageResponse<DeckResponse> expectedResponse = new PageResponse<>(
                    List.of(createSampleDeckResponse(1L, "Deck 1", DeckStatus.PUBLISHED)),
                    1, 10, 1, 1, true
            );

            when(deckService.searchDecksForAdmin(request, pageable)).thenReturn(expectedResponse);

            PageResponse<DeckResponse> actual = adminDeckService.searchDecks(request, pageable);

            assertThat(actual).isEqualTo(expectedResponse);
            verify(deckService).searchDecksForAdmin(request, pageable);
        }

        @Test
        @DisplayName("Should pass sanitized pageable when sort is allowed")
        void searchDecks_WhenSortAllowed_Success() {
            AdminDeckSearchRequest request = new AdminDeckSearchRequest("test", OwnerType.SYSTEM, DeckStatus.PUBLISHED, false, Set.of(), Set.of());
            Pageable sortedPageable = PageRequest.of(0, 10, Sort.by("title"));
            PageResponse<DeckResponse> expectedResponse = new PageResponse<>(List.of(), 1, 10, 0, 0, true);

            when(deckService.searchDecksForAdmin(request, sortedPageable)).thenReturn(expectedResponse);

            PageResponse<DeckResponse> actual = adminDeckService.searchDecks(request, sortedPageable);

            assertThat(actual).isEqualTo(expectedResponse);
            verify(deckService).searchDecksForAdmin(request, sortedPageable);
        }

        @Test
        @DisplayName("Should throw INVALID_SORT when sort field is not allowed")
        void searchDecks_WhenSortNotAllowed_ShouldThrowInvalidSort() {
            AdminDeckSearchRequest request = new AdminDeckSearchRequest("test", OwnerType.SYSTEM, DeckStatus.PUBLISHED, false, Set.of(), Set.of());
            Pageable invalidSortPageable = PageRequest.of(0, 10, Sort.by("unsupportedField"));

            assertThatThrownBy(() -> adminDeckService.searchDecks(request, invalidSortPageable))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_SORT));
        }
    }

    @Nested
    @DisplayName("getDeckById")
    class GetDeckByIdTests {

        @Test
        @DisplayName("Get deck by ID successfully delegating to deckService")
        void getDeckById_Success() {
            DeckResponse expected = createSampleDeckResponse(10L, "Sample Deck", DeckStatus.PUBLISHED);
            when(deckService.getDeckByIdForAdmin(10L)).thenReturn(expected);

            DeckResponse actual = adminDeckService.getDeckById(10L);

            assertThat(actual).isEqualTo(expected);
            verify(deckService).getDeckByIdForAdmin(10L);
        }
    }

    @Nested
    @DisplayName("createSystemDeck")
    class CreateSystemDeckTests {

        @Test
        @DisplayName("Create system deck successfully delegating to deckService")
        void createSystemDeck_Success() {
            SystemDeckRequest request = new SystemDeckRequest("New System Deck", "Desc", null, false, Set.of(), Set.of());
            DeckResponse expected = createSampleDeckResponse(100L, "New System Deck", DeckStatus.DRAFT);
            when(deckService.createSystemDeck(request)).thenReturn(expected);

            DeckResponse actual = adminDeckService.createSystemDeck(request);

            assertThat(actual).isEqualTo(expected);
            verify(deckService).createSystemDeck(request);
        }
    }

    @Nested
    @DisplayName("updateSystemDeck")
    class UpdateSystemDeckTests {

        @Test
        @DisplayName("Update system deck successfully delegating to deckService")
        void updateSystemDeck_Success() {
            SystemDeckRequest request = new SystemDeckRequest("Updated System Deck", "Desc", null, true, Set.of(), Set.of());
            DeckResponse expected = createSampleDeckResponse(100L, "Updated System Deck", DeckStatus.DRAFT);
            when(deckService.updateSystemDeck(100L, request)).thenReturn(expected);

            DeckResponse actual = adminDeckService.updateSystemDeck(100L, request);

            assertThat(actual).isEqualTo(expected);
            verify(deckService).updateSystemDeck(100L, request);
        }
    }

    @Nested
    @DisplayName("publishSystemDeck")
    class PublishSystemDeckTests {

        @Test
        @DisplayName("Publish system deck successfully")
        void publishSystemDeck_Success() {
            DeckResponse expected = createSampleDeckResponse(50L, "Published Deck", DeckStatus.PUBLISHED);
            when(deckService.updateDeckStatus(eq(50L), any(UpdateDeckStatusRequest.class))).thenReturn(expected);

            DeckResponse actual = adminDeckService.publishSystemDeck(50L);

            assertThat(actual).isEqualTo(expected);
            verify(deckService).updateDeckStatus(eq(50L), eq(new UpdateDeckStatusRequest(DeckStatus.PUBLISHED)));
        }
    }

    @Nested
    @DisplayName("archiveSystemDeck")
    class ArchiveSystemDeckTests {

        @Test
        @DisplayName("Archive system deck successfully")
        void archiveSystemDeck_Success() {
            DeckResponse expected = createSampleDeckResponse(50L, "Archived Deck", DeckStatus.ARCHIVED);
            when(deckService.updateDeckStatus(eq(50L), any(UpdateDeckStatusRequest.class))).thenReturn(expected);

            DeckResponse actual = adminDeckService.archiveSystemDeck(50L);

            assertThat(actual).isEqualTo(expected);
            verify(deckService).updateDeckStatus(eq(50L), eq(new UpdateDeckStatusRequest(DeckStatus.ARCHIVED)));
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatusTests {

        @Test
        @DisplayName("Update status to PUBLISHED routes to publishSystemDeck")
        void updateStatus_Published_Success() {
            DeckResponse expected = createSampleDeckResponse(50L, "Deck", DeckStatus.PUBLISHED);
            when(deckService.updateDeckStatus(eq(50L), any(UpdateDeckStatusRequest.class))).thenReturn(expected);

            DeckResponse actual = adminDeckService.updateStatus(50L, new UpdateDeckStatusRequest(DeckStatus.PUBLISHED));

            assertThat(actual).isEqualTo(expected);
            verify(deckService).updateDeckStatus(eq(50L), eq(new UpdateDeckStatusRequest(DeckStatus.PUBLISHED)));
        }

        @Test
        @DisplayName("Update status to ARCHIVED routes to archiveSystemDeck")
        void updateStatus_Archived_Success() {
            DeckResponse expected = createSampleDeckResponse(50L, "Deck", DeckStatus.ARCHIVED);
            when(deckService.updateDeckStatus(eq(50L), any(UpdateDeckStatusRequest.class))).thenReturn(expected);

            DeckResponse actual = adminDeckService.updateStatus(50L, new UpdateDeckStatusRequest(DeckStatus.ARCHIVED));

            assertThat(actual).isEqualTo(expected);
            verify(deckService).updateDeckStatus(eq(50L), eq(new UpdateDeckStatusRequest(DeckStatus.ARCHIVED)));
        }

        @Test
        @DisplayName("Update status to DRAFT routes to deckService directly")
        void updateStatus_Draft_Success() {
            DeckResponse expected = createSampleDeckResponse(50L, "Deck", DeckStatus.DRAFT);
            UpdateDeckStatusRequest request = new UpdateDeckStatusRequest(DeckStatus.DRAFT);
            when(deckService.updateDeckStatus(50L, request)).thenReturn(expected);

            DeckResponse actual = adminDeckService.updateStatus(50L, request);

            assertThat(actual).isEqualTo(expected);
            verify(deckService).updateDeckStatus(50L, request);
        }
    }

    @Nested
    @DisplayName("deleteUserDeck")
    class DeleteUserDeckTests {

        @Test
        @DisplayName("Delete user deck by admin successfully delegating to deckService")
        void deleteUserDeck_Success() {
            adminDeckService.deleteUserDeck(77L, "Terms violation");

            verify(deckService).deleteUserDeckByAdmin(77L, "Terms violation");
        }
    }
}
