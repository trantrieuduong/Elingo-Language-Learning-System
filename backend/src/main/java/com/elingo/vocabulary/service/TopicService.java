package com.elingo.vocabulary.service;

import com.elingo.vocabulary.dto.response.CardResponse;

import java.util.List;

public interface TopicService {
    List<CardResponse> getCardsByTopic(Long userId, Long topicId);
}