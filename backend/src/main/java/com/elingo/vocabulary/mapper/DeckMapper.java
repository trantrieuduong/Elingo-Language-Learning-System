package com.elingo.vocabulary.mapper;

import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.entity.Deck;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {TagMapper.class, CefrLevelMapper.class})
public interface DeckMapper {
    DeckResponse toDeckResponse(Deck deck);
}