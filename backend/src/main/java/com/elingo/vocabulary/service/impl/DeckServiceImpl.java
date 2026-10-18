package com.elingo.vocabulary.service.impl;

import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.progress.service.ProgressService;
import com.elingo.vocabulary.dto.request.DeckSearchRequest;
import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;
import com.elingo.vocabulary.entity.Deck;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.Topic;
import com.elingo.vocabulary.mapper.DeckMapper;
import com.elingo.vocabulary.mapper.TopicMapper;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.repository.TopicRepository;
import com.elingo.vocabulary.repository.specification.DeckFilter;
import com.elingo.vocabulary.repository.specification.DeckSpecifications;
import com.elingo.vocabulary.service.DeckAccessPolicy;
import com.elingo.vocabulary.service.DeckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "DECK-SERVICE")
public class DeckServiceImpl implements DeckService {

    private final DeckRepository deckRepository;
    private final TopicRepository topicRepository;
    private final TopicMapper topicMapper;
    private final DeckMapper deckMapper;
    private final ProgressService progressService;
    private final DeckAccessPolicy accessPolicy;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DeckResponse> getPublishedDecks(DeckSearchRequest req,
                                                        Pageable pageable,
                                                        Long userId) {
        DeckFilter f = DeckFilter.fromRequest(req)
                .statuses(Set.of(DeckStatus.PUBLISHED))
                .build();
        Page<Deck> page = deckRepository.findAll(DeckSpecifications.from(f), pageable);

        Map<Long, Integer> learned = userId == null
                ? Map.of()
                : progressService.getLearnedCardCountByDeck(
                        userId, page.getContent().stream().map(Deck::getId).toList());

        return PageResponse.of(page.map(deck -> deckMapper.toDeckResponse(deck,
                userId == null ? null : learned.getOrDefault(deck.getId(), 0)))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopicResponse> getTopicsByDeck(Long deckId, Long userId) {
        DeckRepository.DeckAccessView deck = deckRepository.findAccessViewById(deckId)
                .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));

        accessPolicy.assertCanLearn(deck, userId);

        List<Topic> topics = topicRepository.findByDeckId(deck.getId(),
                Sort.by("order").and(Sort.by("id")));

        if (topics.isEmpty()) return List.of();

        Map<Long, Integer> learned = userId == null
                ? Map.of()
                : progressService.getLearnedCardCountByTopic(
                        userId, topics.stream().map(Topic::getId).toList());

        return topics.stream()
                .map(t -> topicMapper
                        .toTopicResponse(t, userId == null ? null : learned.getOrDefault(t.getId(), 0)))
                .toList();
    }
}