package com.elingo.learning.lesson.service;

import com.elingo.learning.lesson.dto.request.CreateLessonSegmentRequest;
import com.elingo.learning.lesson.dto.request.DeleteLessonSegmentsRequest;
import com.elingo.learning.lesson.dto.request.UpdateLessonSegmentRequest;
import com.elingo.learning.lesson.dto.response.AdminLessonSegmentResponse;

import java.util.List;

public interface LessonSegmentService {
    List<AdminLessonSegmentResponse> getSegmentsByLessonIdForAdmin(Long lessonId);
    AdminLessonSegmentResponse createSegment(CreateLessonSegmentRequest request);
    AdminLessonSegmentResponse updateSegment(Long segmentId, UpdateLessonSegmentRequest request);
    void deleteSegments(Long lessonId, DeleteLessonSegmentsRequest request);
}
