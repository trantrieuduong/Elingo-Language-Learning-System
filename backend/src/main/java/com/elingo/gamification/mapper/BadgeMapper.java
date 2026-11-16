package com.elingo.gamification.mapper;

import com.elingo.gamification.dto.response.UserBadgeEarnedResponse;
import com.elingo.gamification.entity.Badge;
import org.mapstruct.Mapper;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface BadgeMapper {
    UserBadgeEarnedResponse toBadgeResponse(Badge badge, LocalDateTime earnedAt);
}
