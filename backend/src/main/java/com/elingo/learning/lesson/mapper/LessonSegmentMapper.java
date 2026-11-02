package com.elingo.learning.lesson.mapper;

import com.elingo.learning.lesson.dto.response.LessonSegmentResponse;
import com.elingo.learning.lesson.dto.response.SegmentProgressResponse;
import com.elingo.learning.lesson.entity.LessonSegment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LessonSegmentMapper {

    LessonSegmentResponse toLessonSegmentResponse(LessonSegment segment, SegmentProgressResponse userProgress);
}
