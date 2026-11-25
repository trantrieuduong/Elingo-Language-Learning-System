package com.elingo.admin.content.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.learning.lesson.dto.request.AdminLessonSearchRequest;
import com.elingo.learning.lesson.dto.request.LessonRequest;
import com.elingo.learning.lesson.dto.response.AdminLessonResponse;
import org.springframework.data.domain.Pageable;

public interface AdminLessonService {
    PageResponse<AdminLessonResponse> searchLessons(AdminLessonSearchRequest request, Pageable pageable);
    AdminLessonResponse getLessonById(Long lessonId);
    AdminLessonResponse createLesson(Long adminId, LessonRequest request);
    AdminLessonResponse updateLesson(Long adminId, Long lessonId, LessonRequest request);
    AdminLessonResponse publishLesson(Long adminId, Long lessonId);
    AdminLessonResponse unpublishLesson(Long adminId, Long lessonId);
    AdminLessonResponse archiveLesson(Long adminId, Long lessonId);
}
