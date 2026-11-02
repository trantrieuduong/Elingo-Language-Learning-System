package com.elingo.community.mapper;

import com.elingo.community.dto.response.PostGroupResponse;
import com.elingo.community.entity.PostGroup;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PostGroupMapper {
    PostGroupResponse toPostGroupResponse(PostGroup postGroup);
}
