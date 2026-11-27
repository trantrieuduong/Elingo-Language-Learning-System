package com.elingo.community.mapper;

import com.elingo.community.dto.request.AdminPostGroupRequest;
import com.elingo.community.dto.response.AdminPostGroupResponse;
import com.elingo.community.dto.response.PostGroupResponse;
import com.elingo.community.entity.PostGroup;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PostGroupMapper {
    PostGroupResponse toPostGroupResponse(PostGroup postGroup);

    @Mapping(target = "postCount", source = "postCount")
    AdminPostGroupResponse toAdminResponse(PostGroup postGroup, long postCount);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PostGroup toEntity(AdminPostGroupRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget PostGroup postGroup, AdminPostGroupRequest request);
}
