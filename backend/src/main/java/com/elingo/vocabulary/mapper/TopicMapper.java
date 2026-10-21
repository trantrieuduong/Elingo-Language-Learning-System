package com.elingo.vocabulary.mapper;

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

    void update(@MappingTarget Topic topic, UpdateUserTopicRequest request);
}
