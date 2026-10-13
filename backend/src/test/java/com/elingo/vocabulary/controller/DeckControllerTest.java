package com.elingo.vocabulary.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.PageResponse;
import com.elingo.vocabulary.dto.request.GetPublishedDecksRequest;
import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
import com.elingo.vocabulary.service.DeckService;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class DeckControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DeckService deckService;

    @InjectMocks
    private DeckController deckController;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(deckController)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(@NonNull MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(CurrentUserId.class);
                    }

                    @Override
                    public Object resolveArgument(@NonNull MethodParameter parameter,
                                                  ModelAndViewContainer mavContainer,
                                                  @NonNull NativeWebRequest webRequest,
                                                  WebDataBinderFactory binderFactory) {
                        return USER_ID;
                    }
                })
                .build();
    }

    private DeckResponse buildDeckResponse(String title) {
        return new DeckResponse(1L, title, "slug", "desc", false, null,
                DeckStatus.PUBLISHED, OwnerType.SYSTEM, 0, 0, Set.of(), Set.of());
    }

    private PageResponse<DeckResponse> singlePageOf(DeckResponse... items) {
        List<DeckResponse> content = List.of(items);
        return new PageResponse<>(content, 0, 9, content.size(), 1, true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getPublishedDecks")
    class GetPublishedDecksTests {

        @Test
        @DisplayName("Get published decks successfully")
        void getPublishedDecks_Success() throws Exception {
            DeckResponse deck1 = buildDeckResponse("Animals");
            DeckResponse deck2 = buildDeckResponse("Colors");
            when(deckService.getAllPublishedDecks(eq(USER_ID), any(GetPublishedDecksRequest.class)))
                    .thenReturn(singlePageOf(deck1, deck2));

            mockMvc.perform(get("/decks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(2))
                    .andExpect(jsonPath("$.data.content[0].title").value("Animals"))
                    .andExpect(jsonPath("$.data.content[1].title").value("Colors"))
                    .andExpect(jsonPath("$.data.size").value(9))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.totalElements").value(2))
                    .andExpect(jsonPath("$.data.isLast").value(true));

            verify(deckService).getAllPublishedDecks(eq(USER_ID), any(GetPublishedDecksRequest.class));
        }

        @Test
        @DisplayName("Get published decks successfully: returns empty when no decks found")
        void getPublishedDecks_Success_EmptyPage() throws Exception {
            when(deckService.getAllPublishedDecks(eq(USER_ID), any(GetPublishedDecksRequest.class)))
                    .thenReturn(new PageResponse<>(List.of(), 0, 9, 0, 0, true));

            mockMvc.perform(get("/decks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(0))
                    .andExpect(jsonPath("$.data.totalElements").value(0));

            verify(deckService).getAllPublishedDecks(eq(USER_ID), any(GetPublishedDecksRequest.class));
        }

        @Test
        @DisplayName("Get published decks successfully: filter by CEFR code query param is forwarded")
        void getPublishedDecks_Success_WithCefrFilter() throws Exception {
            DeckResponse deck = buildDeckResponse("B2 Vocabulary");
            when(deckService.getAllPublishedDecks(eq(USER_ID), any(GetPublishedDecksRequest.class)))
                    .thenReturn(singlePageOf(deck));

            mockMvc.perform(get("/decks").param("cefrCode", "B2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("B2 Vocabulary"));

            verify(deckService).getAllPublishedDecks(eq(USER_ID), any(GetPublishedDecksRequest.class));
        }

        @Test
        @DisplayName("Get published decks successfully: filter by tag code query param is forwarded")
        void getPublishedDecks_Success_WithTagFilter() throws Exception {
            when(deckService.getAllPublishedDecks(eq(USER_ID), any(GetPublishedDecksRequest.class)))
                    .thenReturn(new PageResponse<>(List.of(), 0, 9, 0, 0, true));

            mockMvc.perform(get("/decks").param("tagCode", "TRAVEL"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(0));

            verify(deckService).getAllPublishedDecks(eq(USER_ID), any(GetPublishedDecksRequest.class));
        }

        @Test
        @DisplayName("Get published decks successfully: filter by keyword query param is forwarded")
        void getPublishedDecks_Success_WithKeywordFilter() throws Exception {
            DeckResponse deck = buildDeckResponse("English Animals");
            when(deckService.getAllPublishedDecks(eq(USER_ID), any(GetPublishedDecksRequest.class)))
                    .thenReturn(singlePageOf(deck));

            mockMvc.perform(get("/decks").param("keyword", "animals"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1));

            verify(deckService).getAllPublishedDecks(eq(USER_ID), any(GetPublishedDecksRequest.class));
        }
    }
}
