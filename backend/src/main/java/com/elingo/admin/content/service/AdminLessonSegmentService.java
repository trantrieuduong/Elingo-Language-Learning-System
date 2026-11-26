package com.elingo.admin.content.service;

import com.elingo.learning.lesson.dto.request.CreateLessonSegmentRequest;
import com.elingo.learning.lesson.dto.request.DeleteLessonSegmentsRequest;
import com.elingo.learning.lesson.dto.request.UpdateLessonSegmentRequest;
import com.elingo.learning.lesson.dto.response.AdminLessonSegmentResponse;

import java.util.List;

public interface AdminLessonSegmentService {
    List<AdminLessonSegmentResponse> getSegmentsByLessonId(Long lessonId);
    AdminLessonSegmentResponse createSegment(Long adminId, CreateLessonSegmentRequest request);
    AdminLessonSegmentResponse updateSegment(Long adminId, Long segmentId, UpdateLessonSegmentRequest request);
    void deleteSegments(Long adminId, Long lessonId, DeleteLessonSegmentsRequest request);
}
