package com.elingo.learning.lesson.mapper;

import com.elingo.learning.lesson.dto.response.AdminLessonSegmentResponse;
import com.elingo.learning.lesson.dto.response.LessonSegmentResponse;
import com.elingo.learning.lesson.dto.response.SegmentProgressResponse;
import com.elingo.learning.lesson.entity.LessonSegment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LessonSegmentMapper {

    LessonSegmentResponse toLessonSegmentResponse(LessonSegment segment, SegmentProgressResponse userProgress);

    @Mapping(target = "lessonId", source = "lesson.id")
    AdminLessonSegmentResponse toAdminLessonSegmentResponse(LessonSegment segment);
}
