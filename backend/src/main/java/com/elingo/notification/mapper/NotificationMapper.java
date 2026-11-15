package com.elingo.notification.mapper;

import com.elingo.notification.dto.response.NotificationResponse;
import com.elingo.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    @Mapping(target = "titleKey", expression = "java(n.getType().titleKey())")
    @Mapping(target = "read", source = "isRead")
    NotificationResponse toResponse(Notification n);
}
