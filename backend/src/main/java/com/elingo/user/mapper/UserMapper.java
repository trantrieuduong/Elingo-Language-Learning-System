package com.elingo.user.mapper;

import com.elingo.auth.dto.request.RegisterRequest;
import com.elingo.user.dto.response.UserMeResponse;
import com.elingo.user.dto.response.UserPublicResponse;
import com.elingo.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "hasPassword", expression = "java(user.getPassword() != null)")
    @Mapping(target = "linkedGoogle", expression = "java(user.getGoogleProviderId() != null)")
    UserMeResponse toUserMeResponse(User user);

    UserPublicResponse toUserPublicResponse(User user);
    User toUser(RegisterRequest request);
}
