package com.elingo.vocabulary.mapper;

import com.elingo.learning.flashcard.dto.response.CardLearningState;
import com.elingo.vocabulary.dto.request.CreateUserCardRequest;
import com.elingo.vocabulary.dto.request.UpdateUserCardRequest;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.entity.Card;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {CardPhoneticMapper.class})
public interface CardMapper {
    @Mapping(target = "state", source = "state")
    CardResponse toCardResponse(Card card, CardLearningState state);

    CardResponse toCardResponse(Card card);

    @Mapping(target = "topic.id", ignore = true)
    Card toCard(CreateUserCardRequest request);

    void updateCard(@MappingTarget Card card, UpdateUserCardRequest request);
}
