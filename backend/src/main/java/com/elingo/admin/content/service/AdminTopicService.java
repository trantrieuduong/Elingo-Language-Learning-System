package com.elingo.admin.content.service;

import com.elingo.vocabulary.dto.request.CreateSystemTopicRequest;
import com.elingo.vocabulary.dto.request.ReorderItemRequest;
import com.elingo.vocabulary.dto.request.UpdateSystemTopicRequest;
import com.elingo.vocabulary.dto.response.TopicResponse;

import java.util.List;

public interface AdminTopicService {
    List<TopicResponse> getTopicsByDeckId(Long deckId);

    TopicResponse createSystemTopic(CreateSystemTopicRequest request);

    TopicResponse updateSystemTopic(Long topicId, UpdateSystemTopicRequest request);

    void reorderSystemTopics(Long deckId, ReorderItemRequest request);

    void deleteSystemTopic(Long topicId);
}
