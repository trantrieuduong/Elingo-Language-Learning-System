package com.elingo.vocabulary.mapper;

import com.elingo.vocabulary.dto.request.CreateSystemTopicRequest;
import com.elingo.vocabulary.dto.request.UpdateSystemTopicRequest;
import com.elingo.vocabulary.dto.request.UpdateUserTopicRequest;
import com.elingo.vocabulary.dto.response.TopicResponse;
import com.elingo.vocabulary.entity.Topic;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TopicMapper {
    @Mapping(target = "learnedCardCount", source = "learnedCardCount")
    TopicResponse toTopicResponse(Topic topic, Integer learnedCardCount);

    TopicResponse toTopicResponse(Topic topic);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deck", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "cardCount", ignore = true)
    void update(@MappingTarget Topic topic, UpdateUserTopicRequest request);

    Topic toTopic(CreateSystemTopicRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deck", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "cardCount", ignore = true)
    void updateSystemTopic(@MappingTarget Topic topic, UpdateSystemTopicRequest request);
}
