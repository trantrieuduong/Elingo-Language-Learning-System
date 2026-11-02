package com.elingo.vocabulary.mapper;

import com.elingo.vocabulary.dto.response.TagResponse;
import com.elingo.vocabulary.entity.Tag;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TagMapper {
    TagResponse toTagResponse(Tag tag);
}
