package com.elingo.vocabulary.mapper;

import com.elingo.vocabulary.dto.response.TopicResponse;
import com.elingo.vocabulary.entity.Topic;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TopicMapper {
    @Mapping(target = "learnedCardCount", source = "learnedCardCount")
    TopicResponse toTopicResponse(Topic topic, Integer learnedCardCount);
}
