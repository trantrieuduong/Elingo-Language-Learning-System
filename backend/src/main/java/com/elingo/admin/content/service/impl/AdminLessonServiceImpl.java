package com.elingo.admin.content.service.impl;

import com.elingo.admin.common.util.AdminPageables;
import com.elingo.admin.content.service.AdminLessonService;
import com.elingo.common.dto.PageResponse;
import com.elingo.learning.lesson.dto.request.AdminLessonSearchRequest;
import com.elingo.learning.lesson.dto.response.AdminLessonResponse;
import com.elingo.learning.lesson.service.LessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ADMIN-LESSON-SERVICE")
public class AdminLessonServiceImpl implements AdminLessonService {

    // Các trường được phép sử dụng để sắp xếp danh sách bài học
    private static final Set<String> ALLOWED_SORTS = Set.of("id", "createdAt", "title", "publishedAt");

    private final LessonService lessonService;

    @Override
    public PageResponse<AdminLessonResponse> searchLessons(AdminLessonSearchRequest request, Pageable pageable) {
        return lessonService.searchLessonsForAdmin(request, AdminPageables.sanitize(pageable, ALLOWED_SORTS));
    }

    @Override
    public AdminLessonResponse getLessonById(Long lessonId) {
        return lessonService.getLessonByIdForAdmin(lessonId);
    }
}
