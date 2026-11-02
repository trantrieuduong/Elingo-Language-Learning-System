package com.elingo.vocabulary.mapper;

import com.elingo.vocabulary.dto.response.CardPhoneticResponse;
import com.elingo.vocabulary.entity.CardPhonetic;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CardPhoneticMapper {
    CardPhoneticResponse toCardPhoneticResponse(CardPhonetic cardPhonetic);
}
