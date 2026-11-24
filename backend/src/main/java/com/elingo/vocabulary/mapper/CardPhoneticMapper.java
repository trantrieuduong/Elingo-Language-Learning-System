package com.elingo.vocabulary.mapper;

import com.elingo.vocabulary.dto.request.CardPhoneticRequest;
import com.elingo.vocabulary.dto.response.CardPhoneticResponse;
import com.elingo.vocabulary.entity.CardPhonetic;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CardPhoneticMapper {
    CardPhoneticResponse toCardPhoneticResponse(CardPhonetic cardPhonetic);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "card", ignore = true)
    @Mapping(target = "audioUrl", ignore = true)
    @Mapping(target = "locale", source = "locale", defaultValue = "en-US")
    CardPhonetic toCardPhonetic(CardPhoneticRequest request);
}
