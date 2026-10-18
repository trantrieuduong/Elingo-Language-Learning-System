package com.elingo.vocabulary.mapper;

import com.elingo.learning.flashcard.dto.response.CardLearningState;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.entity.Card;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CardPhoneticMapper.class})
public interface CardMapper {
    @Mapping(target = "state", source = "state")
    CardResponse toCardResponse(Card card, CardLearningState state);
}
