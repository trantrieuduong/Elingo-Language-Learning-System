package com.elingo.vocabulary.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.vocabulary.dto.request.DeckSearchRequest;
import com.elingo.vocabulary.dto.request.UserDeckPublicationRequest;
import com.elingo.vocabulary.dto.request.UserDeckRequest;
import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
import com.elingo.vocabulary.service.DeckService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Long USER_ID = 1L;
    private static final Long DECK_ID = 10L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(deckController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver(),
                        new HandlerMethodArgumentResolver() {
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

    // ─────────────────────── helpers ───────────────────────

    private DeckResponse buildDeckResponse(String title) {
        return new DeckResponse(DECK_ID, title, "slug", "desc", false, null,
                DeckStatus.PUBLISHED, OwnerType.SYSTEM, null, 1, 10, 0, LocalDateTime.now(),
                Set.of(), Set.of());
    }

    private PageResponse<DeckResponse> singlePageOf(DeckResponse... items) {
        List<DeckResponse> content = List.of(items);
        return new PageResponse<>(content, 1, 20, content.size(), 1, true);
    }

    private TopicResponse buildTopicResponse(long id, String name) {
        return new TopicResponse(id, name, name.toLowerCase(), 1, 10, 0);
    }

    private String deckBody(String title, String description) throws Exception {
        return objectMapper.writeValueAsString(new UserDeckRequest(title, description));
    }

    // ─────────────────────── getPublishedDecks ───────────────────────

    @Nested
    @DisplayName("getPublishedDecks")
    class GetPublishedDecksTests {

        @Test
        @DisplayName("Get published decks successfully")
        void getPublishedDecks_Success() throws Exception {
            when(deckService.getPublishedDecks(any(DeckSearchRequest.class), any(Pageable.class), eq(USER_ID)))
                    .thenReturn(singlePageOf(buildDeckResponse("Animals"), buildDeckResponse("Colors")));

            mockMvc.perform(get("/decks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(2))
                    .andExpect(jsonPath("$.data.content[0].title").value("Animals"))
                    .andExpect(jsonPath("$.data.content[1].title").value("Colors"))
                    .andExpect(jsonPath("$.data.size").value(20))
                    .andExpect(jsonPath("$.data.page").value(1))
                    .andExpect(jsonPath("$.data.totalElements").value(2))
                    .andExpect(jsonPath("$.data.isLast").value(true));

            verify(deckService).getPublishedDecks(any(DeckSearchRequest.class), any(Pageable.class), eq(USER_ID));
        }

        @Test
        @DisplayName("Get published decks successfully: returns empty page")
        void getPublishedDecks_Success_EmptyPage() throws Exception {
            when(deckService.getPublishedDecks(any(DeckSearchRequest.class), any(Pageable.class), eq(USER_ID)))
                    .thenReturn(new PageResponse<>(List.of(), 1, 20, 0, 0, true));

            mockMvc.perform(get("/decks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(0))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("Get published decks successfully: uses default paging (page 0, size 20, publishedAt desc)")
        void getPublishedDecks_Success_DefaultPaging() throws Exception {
            when(deckService.getPublishedDecks(any(DeckSearchRequest.class), any(Pageable.class), eq(USER_ID)))
                    .thenReturn(singlePageOf());

            mockMvc.perform(get("/decks"))
                    .andExpect(status().isOk());

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(deckService).getPublishedDecks(any(DeckSearchRequest.class), captor.capture(), eq(USER_ID));
            assertThat(captor.getValue().getPageNumber()).isZero();
            assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        }

        @Test
        @DisplayName("Get published decks successfully: custom paging is forwarded")
        void getPublishedDecks_Success_CustomPaging() throws Exception {
            when(deckService.getPublishedDecks(any(DeckSearchRequest.class), any(Pageable.class), eq(USER_ID)))
                    .thenReturn(singlePageOf());

            mockMvc.perform(get("/decks").param("page", "2").param("size", "5"))
                    .andExpect(status().isOk());

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(deckService).getPublishedDecks(any(DeckSearchRequest.class), captor.capture(), eq(USER_ID));
            assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
            assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        }

        @Test
        @DisplayName("Get published decks successfully: filters are bound into the search request")
        void getPublishedDecks_Success_FiltersForwarded() throws Exception {
            when(deckService.getPublishedDecks(any(DeckSearchRequest.class), any(Pageable.class), eq(USER_ID)))
                    .thenReturn(singlePageOf(buildDeckResponse("Travel Words")));

            mockMvc.perform(get("/decks")
                            .param("q", "travel")
                            .param("tags", "TRAVEL", "FOOD")
                            .param("cefr", "B1", "B2")
                            .param("premium", "false")
                            .param("ownerType", "USER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].title").value("Travel Words"));

            ArgumentCaptor<DeckSearchRequest> captor = ArgumentCaptor.forClass(DeckSearchRequest.class);
            verify(deckService).getPublishedDecks(captor.capture(), any(Pageable.class), eq(USER_ID));
            DeckSearchRequest request = captor.getValue();
            assertThat(request.q()).isEqualTo("travel");
            assertThat(request.tags()).containsExactlyInAnyOrder("TRAVEL", "FOOD");
            assertThat(request.cefr()).containsExactlyInAnyOrder("B1", "B2");
            assertThat(request.premium()).isFalse();
            assertThat(request.ownerType()).isEqualTo(OwnerType.USER);
        }

        @Test
        @DisplayName("Get published decks successfully: ownerType defaults to SYSTEM when omitted")
        void getPublishedDecks_Success_DefaultOwnerType() throws Exception {
            when(deckService.getPublishedDecks(any(DeckSearchRequest.class), any(Pageable.class), eq(USER_ID)))
                    .thenReturn(singlePageOf());

            mockMvc.perform(get("/decks"))
                    .andExpect(status().isOk());

            ArgumentCaptor<DeckSearchRequest> captor = ArgumentCaptor.forClass(DeckSearchRequest.class);
            verify(deckService).getPublishedDecks(captor.capture(), any(Pageable.class), eq(USER_ID));
            assertThat(captor.getValue().ownerType()).isEqualTo(OwnerType.SYSTEM);
        }

        @Test
        @DisplayName("Get published decks: Fail because of invalid CEFR code")
        void getPublishedDecks_Fail_InvalidCefrCode() throws Exception {
            mockMvc.perform(get("/decks").param("cefr", "B999"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("CEFR_LEVEL_CODE_INVALID"));

            verify(deckService, never()).getPublishedDecks(any(), any(), any());
        }

        @Test
        @DisplayName("Get published decks: Fail because of blank tag")
        void getPublishedDecks_Fail_BlankTag() throws Exception {
            mockMvc.perform(get("/decks").param("tags", " "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(deckService, never()).getPublishedDecks(any(), any(), any());
        }

        @Test
        @DisplayName("Get published decks: Success when all six CEFR values are requested")
        void getPublishedDecks_Success_AllCefrValues() throws Exception {
            when(deckService.getPublishedDecks(any(DeckSearchRequest.class), any(Pageable.class), eq(USER_ID)))
                    .thenReturn(singlePageOf());

            mockMvc.perform(get("/decks").param("cefr", "A1", "A2", "B1", "B2", "C1", "C2"))
                    .andExpect(status().isOk());

            ArgumentCaptor<DeckSearchRequest> captor = ArgumentCaptor.forClass(DeckSearchRequest.class);
            verify(deckService).getPublishedDecks(captor.capture(), any(Pageable.class), eq(USER_ID));
            assertThat(captor.getValue().cefr())
                    .containsExactlyInAnyOrder("A1", "A2", "B1", "B2", "C1", "C2");
        }

        @Test
        @DisplayName("Get published decks: Fail because of duplicated CEFR values are de-duplicated by the Set")
        void getPublishedDecks_Success_DuplicateCefrValuesDeduplicated() throws Exception {
            when(deckService.getPublishedDecks(any(DeckSearchRequest.class), any(Pageable.class), eq(USER_ID)))
                    .thenReturn(singlePageOf());

            mockMvc.perform(get("/decks").param("cefr", "A1", "A1", "A1", "A1", "A1", "A1", "A1"))
                    .andExpect(status().isOk());

            ArgumentCaptor<DeckSearchRequest> captor = ArgumentCaptor.forClass(DeckSearchRequest.class);
            verify(deckService).getPublishedDecks(captor.capture(), any(Pageable.class), eq(USER_ID));
            assertThat(captor.getValue().cefr()).containsExactly("A1");
        }

        @Test
        @DisplayName("Get published decks: Fail because of too long keyword")
        void getPublishedDecks_Fail_KeywordTooLong() throws Exception {
            mockMvc.perform(get("/decks").param("q", "x".repeat(101)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(deckService, never()).getPublishedDecks(any(), any(), any());
        }
    }

    // ─────────────────────── getDeckById ───────────────────────
    @Nested
    @DisplayName("getDeckById")
    class GetDeckByIdTests {

        @Test
        @DisplayName("Get deck by id successfully")
        void getDeckById_Success() throws Exception {
            when(deckService.getDeckById(DECK_ID, USER_ID)).thenReturn(buildDeckResponse("Animals"));

            mockMvc.perform(get("/decks/{deckId}", DECK_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(DECK_ID))
                    .andExpect(jsonPath("$.data.title").value("Animals"));

            verify(deckService).getDeckById(DECK_ID, USER_ID);
        }

        @Test
        @DisplayName("Get deck by id failed: deck not found")
        void getDeckById_Fail_DeckNotFound() throws Exception {
            when(deckService.getDeckById(DECK_ID, USER_ID))
                    .thenThrow(new AppException(AppError.DECK_NOT_FOUND));

            mockMvc.perform(get("/decks/{deckId}", DECK_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }
    }

    // ─────────────────────── getTopicsByDeckId ───────────────────────

    @Nested
    @DisplayName("getTopicsByDeckId")
    class GetTopicsByDeckIdTests {

        @Test
        @DisplayName("Get topics by deck id successfully")
        void getTopicsByDeckId_Success() throws Exception {
            when(deckService.getTopicsByDeckId(DECK_ID, USER_ID))
                    .thenReturn(List.of(buildTopicResponse(1L, "Animals"), buildTopicResponse(2L, "Colors")));

            mockMvc.perform(get("/decks/{deckId}/topics", DECK_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].name").value("Animals"))
                    .andExpect(jsonPath("$.data[1].name").value("Colors"));

            verify(deckService).getTopicsByDeckId(DECK_ID, USER_ID);
        }

        @Test
        @DisplayName("Get topics by deck id successfully: returns empty list")
        void getTopicsByDeckId_Success_EmptyList() throws Exception {
            when(deckService.getTopicsByDeckId(DECK_ID, USER_ID)).thenReturn(List.of());

            mockMvc.perform(get("/decks/{deckId}/topics", DECK_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @DisplayName("Get topics by deck id: Fail because deck not found")
        void getTopicsByDeckId_Fail_DeckNotFound() throws Exception {
            when(deckService.getTopicsByDeckId(DECK_ID, USER_ID))
                    .thenThrow(new AppException(AppError.DECK_NOT_FOUND));

            mockMvc.perform(get("/decks/{deckId}/topics", DECK_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }

    }

    // ─────────────────────── getTopicsByDeckSlug ───────────────────────

    @Nested
    @DisplayName("getTopicsByDeckSlug")
    class GetTopicsByDeckSlugTests {

        @Test
        @DisplayName("Get topics by deck slug successfully")
        void getTopicsByDeckSlug_Success() throws Exception {
            when(deckService.getTopicsByDeckSlug("oxford-3000", USER_ID))
                    .thenReturn(List.of(buildTopicResponse(1L, "Animals")));

            mockMvc.perform(get("/decks/slug/{slug}/topics", "oxford-3000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].slug").value("animals"));

            verify(deckService).getTopicsByDeckSlug("oxford-3000", USER_ID);
        }

        @Test
        @DisplayName("Get topics by deck slug: Fail because deck not found")
        void getTopicsByDeckSlug_Fail_DeckNotFound() throws Exception {
            when(deckService.getTopicsByDeckSlug("missing", USER_ID))
                    .thenThrow(new AppException(AppError.DECK_NOT_FOUND));

            mockMvc.perform(get("/decks/slug/{slug}/topics", "missing"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }

        @Test
        @DisplayName("Get topics by deck slug: Fail because premium is required")
        void getTopicsByDeckSlug_Fail_PremiumRequired() throws Exception {
            when(deckService.getTopicsByDeckSlug("premium-deck", USER_ID))
                    .thenThrow(new AppException(AppError.PREMIUM_REQUIRED));

            mockMvc.perform(get("/decks/slug/{slug}/topics", "premium-deck"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("PREMIUM_REQUIRED"));
        }
    }

    // ─────────────────────── createUserDeck ───────────────────────

    @Nested
    @DisplayName("createUserDeck")
    class CreateUserDeckTests {

        @Test
        @DisplayName("Create user deck successfully")
        void createUserDeck_Success() throws Exception {
            when(deckService.createUserDeck(eq(USER_ID), any(UserDeckRequest.class)))
                    .thenReturn(buildDeckResponse("My Deck"));

            mockMvc.perform(post("/decks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(deckBody("My Deck", "my description")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("My Deck"));

            ArgumentCaptor<UserDeckRequest> captor = ArgumentCaptor.forClass(UserDeckRequest.class);
            verify(deckService).createUserDeck(eq(USER_ID), captor.capture());
            assertThat(captor.getValue().title()).isEqualTo("My Deck");
            assertThat(captor.getValue().description()).isEqualTo("my description");
        }

        @Test
        @DisplayName("Create user deck successfully: trims title and nulls out blank description")
        void createUserDeck_Success_TrimsInput() throws Exception {
            when(deckService.createUserDeck(eq(USER_ID), any(UserDeckRequest.class)))
                    .thenReturn(buildDeckResponse("My Deck"));

            mockMvc.perform(post("/decks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"  My Deck  \",\"description\":\"   \"}"))
                    .andExpect(status().isOk());

            ArgumentCaptor<UserDeckRequest> captor = ArgumentCaptor.forClass(UserDeckRequest.class);
            verify(deckService).createUserDeck(eq(USER_ID), captor.capture());
            assertThat(captor.getValue().title()).isEqualTo("My Deck");
            assertThat(captor.getValue().description()).isNull();
        }

        @Test
        @DisplayName("Create user deck: Fail because title is blank")
        void createUserDeck_Fail_BlankTitle() throws Exception {
            mockMvc.perform(post("/decks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(deckBody("   ", null)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(deckService, never()).createUserDeck(any(), any());
        }

        @Test
        @DisplayName("Create user deck: Fail because title is missing")
        void createUserDeck_Fail_MissingTitle() throws Exception {
            mockMvc.perform(post("/decks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"description\":\"no title\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(deckService, never()).createUserDeck(any(), any());
        }

        @Test
        @DisplayName("Create user deck: Fail because title is too long")
        void createUserDeck_Fail_TitleTooLong() throws Exception {
            mockMvc.perform(post("/decks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(deckBody("x".repeat(51), null)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(deckService, never()).createUserDeck(any(), any());
        }

        @Test
        @DisplayName("Create user deck: Fail because description is too long")
        void createUserDeck_Fail_DescriptionTooLong() throws Exception {
            mockMvc.perform(post("/decks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(deckBody("My Deck", "x".repeat(256))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(deckService, never()).createUserDeck(any(), any());
        }

        @Test
        @DisplayName("Create user deck: Fail because deck limit is exceeded")
        void createUserDeck_Fail_DeckLimitExceeded() throws Exception {
            when(deckService.createUserDeck(eq(USER_ID), any(UserDeckRequest.class)))
                    .thenThrow(new AppException(AppError.DECK_LIMIT_EXCEEDED));

            mockMvc.perform(post("/decks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(deckBody("My Deck", null)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_LIMIT_EXCEEDED"));
        }
    }

    // ─────────────────────── updateUserDeck ───────────────────────

    @Nested
    @DisplayName("updateUserDeck")
    class UpdateUserDeckTests {

        @Test
        @DisplayName("Update user deck successfully")
        void updateUserDeck_Success() throws Exception {
            when(deckService.updateUserDeck(eq(USER_ID), eq(DECK_ID), any(UserDeckRequest.class)))
                    .thenReturn(buildDeckResponse("Renamed Deck"));

            mockMvc.perform(put("/decks/{deckId}", DECK_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(deckBody("Renamed Deck", "new desc")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("Renamed Deck"));

            verify(deckService).updateUserDeck(eq(USER_ID), eq(DECK_ID), any(UserDeckRequest.class));
        }

        @Test
        @DisplayName("Update user deck: Fail because title is blank")
        void updateUserDeck_Fail_BlankTitle() throws Exception {
            mockMvc.perform(put("/decks/{deckId}", DECK_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(deckBody("", null)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(deckService, never()).updateUserDeck(any(), any(), any());
        }

        @Test
        @DisplayName("Update user deck: Fail because deck is not found")
        void updateUserDeck_Fail_DeckNotFound() throws Exception {
            when(deckService.updateUserDeck(eq(USER_ID), eq(DECK_ID), any(UserDeckRequest.class)))
                    .thenThrow(new AppException(AppError.DECK_NOT_FOUND));

            mockMvc.perform(put("/decks/{deckId}", DECK_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(deckBody("Renamed Deck", null)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }

        @Test
        @DisplayName("Update user deck: Fail because user does not own the deck")
        void updateUserDeck_Fail_AccessDenied() throws Exception {
            when(deckService.updateUserDeck(eq(USER_ID), eq(DECK_ID), any(UserDeckRequest.class)))
                    .thenThrow(new AppException(AppError.DECK_ACCESS_DENIED));

            mockMvc.perform(put("/decks/{deckId}", DECK_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(deckBody("Renamed Deck", null)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_ACCESS_DENIED"));
        }
    }

    // ─────────────────────── deleteUserDeck ───────────────────────

    @Nested
    @DisplayName("deleteUserDeck")
    class DeleteUserDeckTests {

        @Test
        @DisplayName("Delete user deck successfully")
        void deleteUserDeck_Success() throws Exception {
            mockMvc.perform(delete("/decks/{deckId}", DECK_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(deckService).deleteUserDeck(USER_ID, DECK_ID);
        }

        @Test
        @DisplayName("Delete user deck: Fail because deck is not found")
        void deleteUserDeck_Fail_DeckNotFound() throws Exception {
            org.mockito.Mockito.doThrow(new AppException(AppError.DECK_NOT_FOUND))
                    .when(deckService).deleteUserDeck(USER_ID, DECK_ID);

            mockMvc.perform(delete("/decks/{deckId}", DECK_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }

        @Test
        @DisplayName("Delete user deck: Fail because user does not own the deck")
        void deleteUserDeck_Fail_AccessDenied() throws Exception {
            org.mockito.Mockito.doThrow(new AppException(AppError.DECK_ACCESS_DENIED))
                    .when(deckService).deleteUserDeck(USER_ID, DECK_ID);

            mockMvc.perform(delete("/decks/{deckId}", DECK_ID))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_ACCESS_DENIED"));
        }
    }

    // ─────────────────────── setUserDeckPublication ───────────────────────

    @Nested
    @DisplayName("setUserDeckPublication")
    class SetUserDeckPublicationTests {

        @Test
        @DisplayName("Publish user deck successfully")
        void setUserDeckPublication_Publish_Success() throws Exception {
            when(deckService.setUserDeckPublished(USER_ID, DECK_ID, true))
                    .thenReturn(buildDeckResponse("My Deck"));

            String body = objectMapper.writeValueAsString(new UserDeckPublicationRequest(true));

            mockMvc.perform(put("/decks/{deckId}/publication", DECK_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("My Deck"));

            verify(deckService).setUserDeckPublished(USER_ID, DECK_ID, true);
        }

        @Test
        @DisplayName("Unpublish user deck successfully")
        void setUserDeckPublication_Unpublish_Success() throws Exception {
            when(deckService.setUserDeckPublished(USER_ID, DECK_ID, false))
                    .thenReturn(buildDeckResponse("My Deck"));

            String body = objectMapper.writeValueAsString(new UserDeckPublicationRequest(false));

            mockMvc.perform(put("/decks/{deckId}/publication", DECK_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(deckService).setUserDeckPublished(USER_ID, DECK_ID, false);
        }

        @Test
        @DisplayName("Set publication: Fail because published flag is missing")
        void setUserDeckPublication_Fail_MissingFlag() throws Exception {
            mockMvc.perform(put("/decks/{deckId}/publication", DECK_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("INVALID_REQUEST"));

            verify(deckService, never()).setUserDeckPublished(any(), any(), anyBoolean());
        }

        @Test
        @DisplayName("Set publication: Fail because deck is empty")
        void setUserDeckPublication_Fail_DeckEmpty() throws Exception {
            when(deckService.setUserDeckPublished(USER_ID, DECK_ID, true))
                    .thenThrow(new AppException(AppError.DECK_EMPTY));

            String body = objectMapper.writeValueAsString(new UserDeckPublicationRequest(true));

            mockMvc.perform(put("/decks/{deckId}/publication", DECK_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_EMPTY"));
        }

        @Test
        @DisplayName("Set publication: Fail because deck is not found")
        void setUserDeckPublication_Fail_DeckNotFound() throws Exception {
            when(deckService.setUserDeckPublished(USER_ID, DECK_ID, true))
                    .thenThrow(new AppException(AppError.DECK_NOT_FOUND));

            String body = objectMapper.writeValueAsString(new UserDeckPublicationRequest(true));

            mockMvc.perform(put("/decks/{deckId}/publication", DECK_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_NOT_FOUND"));
        }

        @Test
        @DisplayName("Set publication: Fail because user does not own the deck")
        void setUserDeckPublication_Fail_AccessDenied() throws Exception {
            when(deckService.setUserDeckPublished(USER_ID, DECK_ID, true))
                    .thenThrow(new AppException(AppError.DECK_ACCESS_DENIED));

            String body = objectMapper.writeValueAsString(new UserDeckPublicationRequest(true));

            mockMvc.perform(put("/decks/{deckId}/publication", DECK_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("DECK_ACCESS_DENIED"));
        }
    }
}