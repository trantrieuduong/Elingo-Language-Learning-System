package com.elingo.learning.flashcard.mapper;

import com.elingo.learning.flashcard.dto.response.ReviewCardResponse;
import com.elingo.learning.flashcard.entity.UserCardState;
import com.elingo.vocabulary.entity.Card;
import com.elingo.vocabulary.mapper.CardMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CardMapper.class})
public interface FlashcardMapper {

    @Mapping(source = "card", target = "card")
    @Mapping(source = "state.srsNextReviewAt", target = "srsNextReviewAt")
    @Mapping(source = "state.flagsStarred", target = "flagsStarred")
    @Mapping(source = "state.flagsHidden", target = "flagsHidden")
    ReviewCardResponse toReviewCardResponse(Card card, UserCardState state);
}