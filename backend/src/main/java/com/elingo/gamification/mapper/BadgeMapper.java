package com.elingo.gamification.mapper;

import com.elingo.gamification.dto.request.BadgeRequest;
import com.elingo.gamification.dto.response.AdminBadgeResponse;
import com.elingo.gamification.dto.response.UserBadgeEarnedResponse;
import com.elingo.gamification.entity.Badge;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = "spring")
public interface BadgeMapper {

    UserBadgeEarnedResponse toBadgeResponse(Badge badge, LocalDateTime earnedAt);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "iconUrl", ignore = true)
    Badge toBadge(BadgeRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "iconUrl", ignore = true)
    void updateBadge(@MappingTarget Badge badge, BadgeRequest request);

    AdminBadgeResponse toAdminBadgeResponse(Badge badge);

    List<AdminBadgeResponse> toAdminBadgeResponseList(List<Badge> badges);
}
