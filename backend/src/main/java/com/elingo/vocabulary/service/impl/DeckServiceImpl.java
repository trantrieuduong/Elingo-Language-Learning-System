package com.elingo.vocabulary.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.vocabulary.dto.response.TopicResponse;
import com.elingo.vocabulary.mapper.TopicMapper;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.repository.TopicRepository;
import com.elingo.vocabulary.service.DeckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "DECK-SERVICE")
public class DeckServiceImpl implements DeckService {
    private final DeckRepository deckRepository;
    private final TopicRepository topicRepository;
    private final TopicMapper topicMapper;

    @Override
    @Transactional(readOnly = true)
    public List<TopicResponse> getTopicsByDeck(Long userId, Long deckId) {
        log.info("Fetching topics for deckId={}, userId={}", deckId, userId);

        if (!deckRepository.existsById(deckId)) {
            log.warn("Deck not found: deckId={}", deckId);
            throw new AppException(AppError.DECK_NOT_FOUND);
        }

        List<TopicResponse> topics = topicRepository
                .findAllByDeckIdOrderByOrderAsc(deckId)
                .stream()
                .map(topicMapper::toTopicResponse)
                .toList();

        log.info("Fetched {} topic(s) for deckId={}", topics.size(), deckId);
        return topics;
    }
}