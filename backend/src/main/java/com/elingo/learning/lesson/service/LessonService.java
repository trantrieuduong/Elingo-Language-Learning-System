package com.elingo.learning.lesson.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.learning.lesson.dto.request.AdminLessonSearchRequest;
import com.elingo.learning.lesson.dto.request.GetPublishedLessonsRequest;
import com.elingo.learning.lesson.dto.request.LessonRequest;
import com.elingo.learning.lesson.dto.response.AdminLessonResponse;
import com.elingo.learning.lesson.dto.response.LessonDetailResponse;
import com.elingo.learning.lesson.dto.response.LessonResponse;
import com.elingo.learning.lesson.dto.response.LessonSegmentResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LessonService {
    PageResponse<LessonResponse> getAllPublishedLessons(Long userId, GetPublishedLessonsRequest request, Pageable pageable);

    LessonDetailResponse getLessonById(Long userId, Long lessonId);
    LessonDetailResponse getLessonBySlug(Long userId, String slug);

    List<LessonSegmentResponse> getSegmentsByLessonId(Long userId, Long lessonId);
    List<LessonSegmentResponse> getSegmentsByLessonSlug(Long userId, String slug);

    // Admin lesson management
    PageResponse<AdminLessonResponse> searchLessonsForAdmin(AdminLessonSearchRequest request, Pageable pageable);
    AdminLessonResponse getLessonByIdForAdmin(Long lessonId);
    AdminLessonResponse createLesson(LessonRequest request);
    AdminLessonResponse updateLesson(Long lessonId, LessonRequest request);
    AdminLessonResponse publishLesson(Long lessonId);
    AdminLessonResponse unpublishLesson(Long lessonId);
    AdminLessonResponse archiveLesson(Long lessonId);
}
