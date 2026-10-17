package com.elingo.vocabulary.mapper;

import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.entity.Deck;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {TagMapper.class, CefrLevelMapper.class})
public interface DeckMapper {
    @Mapping(target = "learnedCardCount", expression = "java(null)")
    DeckResponse toDeckResponse(Deck deck);

    DeckResponse toDeckResponse(Deck deck, Integer learnedCardCount);
}