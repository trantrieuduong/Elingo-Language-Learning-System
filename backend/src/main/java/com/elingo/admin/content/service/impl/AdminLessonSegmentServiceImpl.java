package com.elingo.admin.content.service.impl;

import com.elingo.admin.audit.annotation.Audited;
import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import com.elingo.admin.content.service.AdminLessonSegmentService;
import com.elingo.learning.lesson.dto.request.CreateLessonSegmentRequest;
import com.elingo.learning.lesson.dto.request.DeleteLessonSegmentsRequest;
import com.elingo.learning.lesson.dto.request.UpdateLessonSegmentRequest;
import com.elingo.learning.lesson.dto.response.AdminLessonSegmentResponse;
import com.elingo.learning.lesson.service.LessonSegmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ADMIN-LESSON-SEGMENT-SERVICE")
public class AdminLessonSegmentServiceImpl implements AdminLessonSegmentService {

    private final LessonSegmentService lessonSegmentService;

    @Override
    public List<AdminLessonSegmentResponse> getSegmentsByLessonId(Long lessonId) {
        return lessonSegmentService.getSegmentsByLessonIdForAdmin(lessonId); // đọc: không audit, không log
    }

    @Override
    @Audited(action = AdminAuditAction.SEGMENT_CREATE,
             target = AdminAuditTargetType.LESSON_SEGMENT,
             targetId = "#result.id",
             detail = "'Lesson: ' + #request.lessonId() + ', ' + #request.startMs() + '-' + #request.endMs()")
    public AdminLessonSegmentResponse createSegment(Long adminId, CreateLessonSegmentRequest request) {
        AdminLessonSegmentResponse created = lessonSegmentService.createSegment(request);
        log.info("Lesson segment created segmentId={} lessonId={} byAdminId={}", created.id(), created.lessonId(), adminId);
        return created;
    }

    @Override
    @Audited(action = AdminAuditAction.SEGMENT_UPDATE,
             target = AdminAuditTargetType.LESSON_SEGMENT,
             targetId = "#segmentId",
             detail = "'Time: ' + #request.startMs() + '-' + #request.endMs()")
    public AdminLessonSegmentResponse updateSegment(Long adminId, Long segmentId, UpdateLessonSegmentRequest request) {
        AdminLessonSegmentResponse updated = lessonSegmentService.updateSegment(segmentId, request);
        log.info("Lesson segment updated segmentId={} byAdminId={}", segmentId, adminId);
        return updated;
    }

    @Override
    @Audited(action = AdminAuditAction.SEGMENT_DELETE,
             target = AdminAuditTargetType.LESSON,
             targetId = "#lessonId",
             detail = "'Deleted segmentIds: ' + #request.segmentIds()")
    public void deleteSegments(Long adminId, Long lessonId, DeleteLessonSegmentsRequest request) {
        lessonSegmentService.deleteSegments(lessonId, request);
        log.info("Lesson segments deleted lessonId={} count={} byAdminId={}", lessonId, request.segmentIds().size(), adminId);
    }
}
