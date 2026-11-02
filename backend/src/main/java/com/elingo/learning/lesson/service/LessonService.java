package com.elingo.learning.lesson.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.learning.lesson.dto.request.GetPublishedLessonsRequest;
import com.elingo.learning.lesson.dto.response.LessonDetailResponse;
import com.elingo.learning.lesson.dto.response.LessonResponse;
import com.elingo.learning.lesson.dto.response.LessonSegmentResponse;

import java.util.List;

public interface LessonService {
    PageResponse<LessonResponse> getAllPublishedLessons(Long userId, GetPublishedLessonsRequest request);

    LessonDetailResponse getLessonById(Long userId, Long lessonId);
    LessonDetailResponse getLessonBySlug(Long userId, String slug);

    List<LessonSegmentResponse> getSegmentsByLessonId(Long userId, Long lessonId);
    List<LessonSegmentResponse> getSegmentsByLessonSlug(Long userId, String slug);
}
