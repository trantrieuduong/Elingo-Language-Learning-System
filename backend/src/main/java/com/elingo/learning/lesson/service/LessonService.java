package com.elingo.learning.lesson.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.learning.lesson.dto.request.GetPublishedLessonsRequest;
import com.elingo.learning.lesson.dto.response.LessonDetailResponse;
import com.elingo.learning.lesson.dto.response.LessonResponse;
import com.elingo.learning.lesson.dto.response.LessonSegmentResponse;

import java.util.List;

public interface LessonService {
    PageResponse<LessonResponse> getAllPublishedLessons(Long userId, GetPublishedLessonsRequest request);

    LessonDetailResponse getLessonBySlug(Long userId, String slug);

    List<LessonSegmentResponse> getSegmentsByLesson(Long userId, String slug);
}
