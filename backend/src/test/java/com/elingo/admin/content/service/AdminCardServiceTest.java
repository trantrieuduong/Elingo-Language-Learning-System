package com.elingo.admin.content.service;

import com.elingo.vocabulary.dto.request.CreateSystemCardRequest;
import com.elingo.vocabulary.dto.request.ReorderItemRequest;
import com.elingo.vocabulary.dto.request.UpdateSystemCardRequest;
import com.elingo.admin.content.service.impl.AdminCardServiceImpl;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.service.CardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AdminCardServiceTest {

    @Mock
    private CardService cardService;

    @InjectMocks
    private AdminCardServiceImpl adminCardService;

    private CardResponse createSampleCardResponse(Long id, String term) {
        return new CardResponse(
                id,
                1,
                term,
                "noun",
                "nghĩa",
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null
        );
    }

    @Nested
    @DisplayName("getCardsByTopicId")
    class GetCardsByTopicIdTests {

        @Test
        @DisplayName("Get cards by topic ID successfully delegating to cardService")
        void getCardsByTopicId_Success() {
            List<CardResponse> expected = List.of(
                    createSampleCardResponse(1L, "word 1"),
                    createSampleCardResponse(2L, "word 2")
            );
            when(cardService.getCardsByTopicIdForAdmin(10L)).thenReturn(expected);

            List<CardResponse> actual = adminCardService.getCardsByTopicId(10L);

            assertThat(actual).isEqualTo(expected);
            verify(cardService).getCardsByTopicIdForAdmin(10L);
        }
    }

    @Nested
    @DisplayName("getCardById")
    class GetCardByIdTests {

        @Test
        @DisplayName("Get card by ID successfully delegating to cardService")
        void getCardById_Success() {
            CardResponse expected = createSampleCardResponse(50L, "term");
            when(cardService.getCardByIdForAdmin(50L)).thenReturn(expected);

            CardResponse actual = adminCardService.getCardById(50L);

            assertThat(actual).isEqualTo(expected);
            verify(cardService).getCardByIdForAdmin(50L);
        }
    }

    @Nested
    @DisplayName("createSystemCard")
    class CreateSystemCardTests {

        @Test
        @DisplayName("Create system card successfully delegating to cardService")
        void createSystemCard_Success() {
            CreateSystemCardRequest request = new CreateSystemCardRequest(
                    10L, "apple", "n", "quả táo", null, null, null, null, null, null
            );
            CardResponse expected = createSampleCardResponse(100L, "apple");
            when(cardService.createSystemCard(request)).thenReturn(expected);

            CardResponse actual = adminCardService.createSystemCard(request);

            assertThat(actual).isEqualTo(expected);
            verify(cardService).createSystemCard(request);
        }
    }

    @Nested
    @DisplayName("updateSystemCard")
    class UpdateSystemCardTests {

        @Test
        @DisplayName("Update system card successfully delegating to cardService")
        void updateSystemCard_Success() {
            UpdateSystemCardRequest request = new UpdateSystemCardRequest(
                    "apple", "n", "quả táo đỏ", null, null, null, null, null, null
            );
            CardResponse expected = createSampleCardResponse(100L, "apple");
            when(cardService.updateSystemCard(100L, request)).thenReturn(expected);

            CardResponse actual = adminCardService.updateSystemCard(100L, request);

            assertThat(actual).isEqualTo(expected);
            verify(cardService).updateSystemCard(100L, request);
        }
    }

    @Nested
    @DisplayName("reorderSystemCards")
    class ReorderSystemCardsTests {

        @Test
        @DisplayName("Reorder system cards successfully delegating to cardService")
        void reorderSystemCards_Success() {
            List<Long> orderedIds = List.of(3L, 2L, 1L);
            ReorderItemRequest request = new ReorderItemRequest(orderedIds);

            adminCardService.reorderSystemCards(10L, request);

            verify(cardService).reorderSystemCards(10L, orderedIds);
        }
    }

    @Nested
    @DisplayName("deleteSystemCard")
    class DeleteSystemCardTests {

        @Test
        @DisplayName("Delete system card successfully delegating to cardService")
        void deleteSystemCard_Success() {
            adminCardService.deleteSystemCard(88L);

            verify(cardService).deleteSystemCard(88L);
        }
    }
}
