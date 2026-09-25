package com.elingo.vocabulary.mapper;

import com.elingo.vocabulary.dto.response.TopicResponse;
import com.elingo.vocabulary.entity.Topic;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TopicMapper {
    TopicResponse toTopicResponse(Topic topic);
}
