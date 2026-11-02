package com.elingo.learning.lesson.mapper;

import com.elingo.learning.lesson.dto.response.LessonDetailResponse;
import com.elingo.learning.lesson.dto.response.LessonProgressSummaryResponse;
import com.elingo.learning.lesson.dto.response.LessonResponse;
import com.elingo.learning.lesson.entity.Lesson;
import com.elingo.vocabulary.mapper.CefrLevelMapper;
import com.elingo.vocabulary.mapper.TagMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {TagMapper.class, CefrLevelMapper.class})
public interface LessonMapper {

    LessonResponse toLessonResponse(Lesson lesson, LessonProgressSummaryResponse userProgress);

    LessonDetailResponse toLessonDetailResponse(Lesson lesson, Integer segmentCount, LessonProgressSummaryResponse userProgress, Integer dictationPassScore, Integer shadowingPassScore);
}
