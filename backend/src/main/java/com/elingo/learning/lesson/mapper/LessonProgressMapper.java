package com.elingo.learning.lesson.mapper;

import com.elingo.learning.lesson.dto.response.LessonProgressSummaryResponse;
import com.elingo.learning.lesson.dto.response.SegmentProgressResponse;
import com.elingo.learning.lesson.dto.response.WordAccuracyResponse;
import com.elingo.learning.lesson.entity.SegmentWordAccuracy;
import com.elingo.learning.lesson.entity.UserLessonProgress;
import com.elingo.learning.lesson.entity.UserSegmentProgress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LessonProgressMapper {

    LessonProgressSummaryResponse toLessonProgressSummaryResponse(UserLessonProgress progress);

    @Mapping(source = "progress.segment.id", target = "segmentId")
    SegmentProgressResponse toSegmentProgressResponse(UserSegmentProgress progress, List<WordAccuracyResponse> wordsAccuracy);

    WordAccuracyResponse toWordAccuracyResponse(SegmentWordAccuracy wordAccuracy);
}
