package com.elingo.vocabulary.mapper;

import com.elingo.vocabulary.dto.response.CefrLevelResponse;
import com.elingo.vocabulary.entity.CefrLevel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CefrLevelMapper {
    CefrLevelResponse toCefrLevelResponse(CefrLevel cefrLevel);
}
