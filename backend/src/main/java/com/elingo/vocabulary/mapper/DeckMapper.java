package com.elingo.vocabulary.mapper;

import com.elingo.vocabulary.dto.response.CefrLevelResponse;
import com.elingo.vocabulary.dto.response.DeckResponse;
import com.elingo.vocabulary.dto.response.TagResponse;
import com.elingo.vocabulary.entity.CefrLevel;
import com.elingo.vocabulary.entity.Deck;
import com.elingo.vocabulary.entity.Tag;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DeckMapper {
    DeckResponse toDeckResponse(Deck deck);

    TagResponse toTagResponse(Tag tag);//

    CefrLevelResponse toCefrLevelResponse(CefrLevel cefrLevel);//
}