package com.elingo.vocabulary.service;

import com.elingo.vocabulary.dto.request.CreateTopicRequest;
import com.elingo.vocabulary.dto.request.UpdateTopicRequest;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;

import java.util.List;

public interface TopicService {
    List<CardResponse> getUnlearnedCardsByTopic(Long userId, Long topicId);

    TopicResponse createUserTopic(CreateTopicRequest request, Long currentUserId);

    TopicResponse createSystemTopic(CreateTopicRequest request);

    TopicResponse updateUserTopic(Long topicId, UpdateTopicRequest request, Long currentUserId);

    TopicResponse updateSystemTopic(Long topicId, UpdateTopicRequest request);

    void deleteTopic(Long topicId, Long currentUserId, boolean isAdmin);
}