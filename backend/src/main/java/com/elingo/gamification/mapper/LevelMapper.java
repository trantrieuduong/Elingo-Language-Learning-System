package com.elingo.gamification.mapper;

import com.elingo.gamification.dto.request.LevelRequest;
import com.elingo.gamification.dto.response.AdminLevelResponse;
import com.elingo.gamification.dto.response.LevelResponse;
import com.elingo.gamification.entity.Level;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LevelMapper {

    LevelResponse toLevelResponse(Level level);

    List<LevelResponse> toLevelResponseList(List<Level> levels);

    @Mapping(target = "levelNo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Level toLevel(LevelRequest request);

    @Mapping(target = "levelNo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateLevel(@MappingTarget Level level, LevelRequest request);

    AdminLevelResponse toAdminLevelResponse(Level level);

    List<AdminLevelResponse> toAdminLevelResponseList(List<Level> levels);
}
