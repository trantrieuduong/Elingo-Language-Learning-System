package com.elingo.learning.lesson.mapper;

import com.elingo.learning.lesson.dto.request.LessonRequest;
import com.elingo.learning.lesson.dto.response.AdminLessonResponse;
import com.elingo.learning.lesson.dto.response.LessonDetailResponse;
import com.elingo.learning.lesson.dto.response.LessonProgressSummaryResponse;
import com.elingo.learning.lesson.dto.response.LessonResponse;
import com.elingo.learning.lesson.entity.Lesson;
import com.elingo.vocabulary.mapper.CefrLevelMapper;
import com.elingo.vocabulary.mapper.TagMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {TagMapper.class, CefrLevelMapper.class})
public interface LessonMapper {

    LessonResponse toLessonResponse(Lesson lesson, LessonProgressSummaryResponse userProgress);

    LessonDetailResponse toLessonDetailResponse(Lesson lesson, Integer segmentCount, LessonProgressSummaryResponse userProgress, Integer dictationPassScore, Integer shadowingPassScore);

    AdminLessonResponse toAdminLessonResponse(Lesson lesson, Integer segmentCount);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "thumbnailUrl", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "cefrLevels", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Lesson toLesson(LessonRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "thumbnailUrl", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "cefrLevels", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateLesson(@MappingTarget Lesson lesson, LessonRequest request);
}
