package com.elingo.admin.content.service.impl;

import com.elingo.admin.audit.annotation.Audited;
import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import com.elingo.admin.common.util.AdminPageables;
import com.elingo.admin.content.service.AdminLessonService;
import com.elingo.common.dto.PageResponse;
import com.elingo.learning.lesson.dto.request.AdminLessonSearchRequest;
import com.elingo.learning.lesson.dto.request.LessonRequest;
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

    @Override
    @Audited(action = AdminAuditAction.LESSON_CREATE, target = AdminAuditTargetType.LESSON,
             targetId = "#result.id", detail = "'Title: ' + #request.title()")
    public AdminLessonResponse createLesson(Long adminId, LessonRequest request) {
        AdminLessonResponse created = lessonService.createLesson(request);
        log.info("Lesson created lessonId={} byAdminId={}", created.id(), adminId);
        return created;
    }

    @Override
    @Audited(action = AdminAuditAction.LESSON_UPDATE, target = AdminAuditTargetType.LESSON,
             targetId = "#lessonId", detail = "'Update: ' + #request.title()")
    public AdminLessonResponse updateLesson(Long adminId, Long lessonId, LessonRequest request) {
        AdminLessonResponse updated = lessonService.updateLesson(lessonId, request);
        log.info("Lesson updated lessonId={} byAdminId={}", lessonId, adminId);
        return updated;
    }

    @Override
    @Audited(action = AdminAuditAction.LESSON_PUBLISH, target = AdminAuditTargetType.LESSON,
             targetId = "#lessonId")
    public AdminLessonResponse publishLesson(Long adminId, Long lessonId) {
        AdminLessonResponse published = lessonService.publishLesson(lessonId);
        log.info("Lesson published lessonId={} byAdminId={}", lessonId, adminId);
        return published;
    }

    @Override
    @Audited(action = AdminAuditAction.LESSON_UNPUBLISH, target = AdminAuditTargetType.LESSON,
             targetId = "#lessonId")
    public AdminLessonResponse unpublishLesson(Long adminId, Long lessonId) {
        AdminLessonResponse unpublished = lessonService.unpublishLesson(lessonId);
        log.info("Lesson unpublished lessonId={} byAdminId={}", lessonId, adminId);
        return unpublished;
    }

    @Override
    @Audited(action = AdminAuditAction.LESSON_ARCHIVE, target = AdminAuditTargetType.LESSON,
             targetId = "#lessonId")
    public AdminLessonResponse archiveLesson(Long adminId, Long lessonId) {
        AdminLessonResponse archived = lessonService.archiveLesson(lessonId);
        log.info("Lesson archived lessonId={} byAdminId={}", lessonId, adminId);
        return archived;
    }
}
