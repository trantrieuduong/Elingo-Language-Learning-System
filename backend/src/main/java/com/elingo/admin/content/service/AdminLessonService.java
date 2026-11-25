package com.elingo.admin.content.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.learning.lesson.dto.request.AdminLessonSearchRequest;
import com.elingo.learning.lesson.dto.response.AdminLessonResponse;
import org.springframework.data.domain.Pageable;

public interface AdminLessonService {
    PageResponse<AdminLessonResponse> searchLessons(AdminLessonSearchRequest request, Pageable pageable);
    AdminLessonResponse getLessonById(Long lessonId);
}
