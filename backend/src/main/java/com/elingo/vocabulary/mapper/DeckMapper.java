package com.elingo.vocabulary.mapper;

import com.elingo.vocabulary.dto.request.UserDeckRequest;
import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.entity.Deck;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {TagMapper.class, CefrLevelMapper.class})
public interface DeckMapper {
    @Mapping(target = "ownerId", source = "deck.owner.id")
    @Mapping(target = "learnedCardCount", source = "learnedCardCount")
    DeckResponse toDeckResponse(Deck deck, Integer learnedCardCount);

    @Mapping(target = "ownerId", source = "deck.owner.id")
    DeckResponse toDeckResponse(Deck deck);

    void updateDeck(@MappingTarget Deck deck, UserDeckRequest request);
}