package com.elingo.vocabulary.mapper;

import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.entity.Deck;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {TagMapper.class, CefrLevelMapper.class})
public interface DeckMapper {
    @Mapping(target = "ownerId", source = "deck.owner.id")
    @Mapping(target = "learnedCardCount", source = "learnedCardCount")
    DeckResponse toDeckResponse(Deck deck, Integer learnedCardCount);
}