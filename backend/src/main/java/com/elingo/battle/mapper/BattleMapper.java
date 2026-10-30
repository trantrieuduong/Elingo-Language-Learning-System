package com.elingo.battle.mapper;

import com.elingo.battle.dto.response.PlayerResponse;
import com.elingo.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BattleMapper {
    @Mapping(target = "userId", source = "id")
    @Mapping(target = "displayName", source = "fullName")
    @Mapping(target = "avatarUrl", source = "avatarUrl")
    @Mapping(target = "isHost", constant = "false")
    PlayerResponse toPlayerResponse(User user);
}
