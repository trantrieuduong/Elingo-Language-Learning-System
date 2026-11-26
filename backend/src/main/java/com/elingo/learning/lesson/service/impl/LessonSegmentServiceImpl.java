package com.elingo.learning.lesson.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.learning.lesson.dto.request.CreateLessonSegmentRequest;
import com.elingo.learning.lesson.dto.request.DeleteLessonSegmentsRequest;
import com.elingo.learning.lesson.dto.request.UpdateLessonSegmentRequest;
import com.elingo.learning.lesson.dto.response.AdminLessonSegmentResponse;
import com.elingo.learning.lesson.entity.Lesson;
import com.elingo.learning.lesson.entity.LessonSegment;
import com.elingo.learning.lesson.entity.LessonStatus;
import com.elingo.learning.lesson.mapper.LessonSegmentMapper;
import com.elingo.learning.lesson.repository.LessonRepository;
import com.elingo.learning.lesson.repository.LessonSegmentRepository;
import com.elingo.learning.lesson.repository.SegmentWordAccuracyRepository;
import com.elingo.learning.lesson.repository.UserSegmentProgressRepository;
import com.elingo.learning.lesson.service.LessonSegmentService;
import com.elingo.learning.lesson.util.TranscriptUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonSegmentServiceImpl implements LessonSegmentService {

    private final LessonRepository lessonRepository;
    private final LessonSegmentRepository lessonSegmentRepository;
    private final UserSegmentProgressRepository userSegmentProgressRepository;
    private final SegmentWordAccuracyRepository segmentWordAccuracyRepository;
    private final LessonSegmentMapper lessonSegmentMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AdminLessonSegmentResponse> getSegmentsByLessonIdForAdmin(Long lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new AppException(AppError.LESSON_NOT_FOUND);
        }
        return lessonSegmentRepository.findAllByLessonIdOrderByStartMsAsc(lessonId).stream()
                .map(lessonSegmentMapper::toAdminLessonSegmentResponse)
                .toList();
    }

    @Override
    @Transactional
    public AdminLessonSegmentResponse createSegment(CreateLessonSegmentRequest request) {
        Lesson lesson = lessonRepository.findById(request.lessonId())
                .orElseThrow(() -> new AppException(AppError.LESSON_NOT_FOUND));

        validateTimeRange(lesson, request.startMs(), request.endMs());

        if (lessonSegmentRepository.existsByLessonIdAndStartMsLessThanAndEndMsGreaterThan(
                lesson.getId(), request.endMs(), request.startMs())) {
            throw new AppException(AppError.SEGMENT_TIME_OVERLAP);
        }

        String normalized = requireNormalized(request.transcriptOriginal());

        LessonSegment segment = LessonSegment.builder()
                .lesson(lesson)
                .startMs(request.startMs())
                .endMs(request.endMs())
                .transcriptOriginal(request.transcriptOriginal())
                .transcriptNormalized(normalized)
                .translation(request.translation())
                .build();

        LessonSegment saved = lessonSegmentRepository.save(segment);
        return lessonSegmentMapper.toAdminLessonSegmentResponse(saved);
    }

    @Override
    @Transactional
    public AdminLessonSegmentResponse updateSegment(Long segmentId, UpdateLessonSegmentRequest request) {
        LessonSegment segment = lessonSegmentRepository.findById(segmentId)
                .orElseThrow(() -> new AppException(AppError.LESSON_SEGMENT_NOT_FOUND));

        Lesson lesson = segment.getLesson(); // LAZY proxy; getId() không phát sinh query thêm

        validateTimeRange(lesson, request.startMs(), request.endMs());

        if (lessonSegmentRepository.existsByLessonIdAndIdNotAndStartMsLessThanAndEndMsGreaterThan(
                lesson.getId(), segmentId, request.endMs(), request.startMs())) {
            throw new AppException(AppError.SEGMENT_TIME_OVERLAP);
        }

        segment.setStartMs(request.startMs());
        segment.setEndMs(request.endMs());
        segment.setTranscriptOriginal(request.transcriptOriginal());
        segment.setTranscriptNormalized(requireNormalized(request.transcriptOriginal()));
        segment.setTranslation(request.translation());

        return lessonSegmentMapper.toAdminLessonSegmentResponse(segment);
    }

    @Override
    @Transactional
    public void deleteSegments(Long lessonId, DeleteLessonSegmentsRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new AppException(AppError.LESSON_NOT_FOUND));
        List<LessonSegment> segments = lessonSegmentRepository.findAllByIdInAndLessonId(request.segmentIds(), lessonId);
        if (segments.size() != request.segmentIds().size()) {
            throw new AppException(AppError.LESSON_SEGMENT_NOT_FOUND);
        }
        assertNotRemovingAllFromPublished(lesson, segments.size());
        List<Long> ids = segments.stream().map(LessonSegment::getId).toList();
        segmentWordAccuracyRepository.deleteAllBySegmentIds(ids);
        userSegmentProgressRepository.deleteAllBySegmentIds(ids);
        lessonSegmentRepository.deleteAll(segments);
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    private void assertNotRemovingAllFromPublished(Lesson lesson, int removeCount) {
        if (lesson.getStatus() == LessonStatus.PUBLISHED
                && lessonSegmentRepository.countByLessonId(lesson.getId()) - removeCount <= 0) {
            throw new AppException(AppError.LESSON_SEGMENT_REQUIRED);
        }
    }

    private void validateTimeRange(Lesson lesson, Integer startMs, Integer endMs) {
        if (startMs >= endMs) {
            throw new AppException(AppError.INVALID_SEGMENT_TIME_RANGE);
        }
        if (lesson.getDurationMs() != null && endMs > lesson.getDurationMs()) {
            throw new AppException(AppError.SEGMENT_EXCEEDS_DURATION);
        }
    }

    private String requireNormalized(String original) {
        String normalized = TranscriptUtils.normalize(original);
        if (normalized.isEmpty()) {
            throw new AppException(AppError.INVALID_REQUEST);
        }
        return normalized;
    }
}
