package com.elingo.vocabulary.service;

import com.elingo.vocabulary.dto.request.CreateUserTopicRequest;
import com.elingo.vocabulary.dto.request.UpdateUserTopicRequest;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.dto.response.TopicResponse;

import java.util.List;

public interface TopicService {
    List<CardResponse> getCardsByTopic(Long topicId, Long userId);

    // CRUD user topic
    TopicResponse createUserTopic(Long userId, CreateUserTopicRequest request);
    TopicResponse updateUserTopic(Long userId, Long topicId, UpdateUserTopicRequest request);
    void deleteUserTopic(Long userId, Long topicId);
}