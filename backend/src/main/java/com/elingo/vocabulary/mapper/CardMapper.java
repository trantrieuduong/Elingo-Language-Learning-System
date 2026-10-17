package com.elingo.vocabulary.mapper;

import com.elingo.vocabulary.dto.response.CardPhoneticResponse;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.entity.Card;
import com.elingo.vocabulary.entity.CardPhonetic;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")// Biến interface thành một Spring Bean
public interface CardMapper {
    // Ánh xạ entity Card -> CardResponse record.
    @Mapping(target = "flagsStarred", expression = "java(null)")
    CardResponse toCardResponse(Card card);

    CardResponse toCardResponse(Card card, Boolean flagsStarred);

    CardPhoneticResponse toCardPhoneticResponse(CardPhonetic phonetic);
}
