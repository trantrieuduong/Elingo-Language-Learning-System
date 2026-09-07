package com.elingo.user.mapper;

import com.elingo.auth.dto.request.RegisterRequest;
import com.elingo.user.dto.response.UserResponse;
import com.elingo.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toUserResponse(User user);
    User toUser(RegisterRequest request);
}
