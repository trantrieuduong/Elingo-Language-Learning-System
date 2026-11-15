package com.elingo.learning.lesson.service.impl;

import com.elingo.common.dto.PageResponse;
import com.elingo.learning.lesson.config.LessonProperties;
import com.elingo.learning.lesson.dto.request.GetPublishedLessonsRequest;
import com.elingo.learning.lesson.dto.response.LessonDetailResponse;
import com.elingo.learning.lesson.dto.response.LessonProgressSummaryResponse;
import com.elingo.learning.lesson.dto.response.LessonResponse;
import com.elingo.learning.lesson.dto.response.LessonSegmentResponse;
import com.elingo.learning.lesson.dto.response.SegmentProgressResponse;
import com.elingo.learning.lesson.entity.Lesson;
import com.elingo.learning.lesson.entity.LessonSegment;
import com.elingo.learning.lesson.mapper.LessonMapper;
import com.elingo.learning.lesson.mapper.LessonSegmentMapper;
import com.elingo.learning.lesson.repository.LessonRepository;
import com.elingo.learning.lesson.repository.LessonSegmentRepository;
import com.elingo.learning.lesson.repository.specification.LessonFilter;
import com.elingo.learning.lesson.repository.specification.LessonSpecifications;
import com.elingo.learning.lesson.service.LessonAccessService;
import com.elingo.learning.lesson.service.LessonService;
import com.elingo.progress.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final LessonSegmentRepository lessonSegmentRepository;
    private final LessonAccessService lessonAccessService;
    private final LessonMapper lessonMapper;
    private final LessonSegmentMapper lessonSegmentMapper;
    private final LessonProperties lessonProperties;
    private final ProgressService progressService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LessonResponse> getAllPublishedLessons(Long userId,
                                                               GetPublishedLessonsRequest request,
                                                               Pageable pageable) {
        // Trim keyword; cefrLevelIds/tagIds null hoặc rỗng = không lọc
        String keyword = StringUtils.hasText(request.keyword()) ? request.keyword().trim() : null;

        // Lấy danh sách bài học theo bộ lọc và phân trang
        Page<Lesson> lessonPage = lessonRepository.findAll(
                LessonSpecifications.withFilter(new LessonFilter(request.cefrLevelIds(), request.tagIds(), keyword)),
                pageable);

        // Lấy tiến độ học của các bài học trong trang hiện tại
        List<Long> lessonIds = lessonPage.getContent().stream().map(Lesson::getId).toList();
        Map<Long, LessonProgressSummaryResponse> progressMap = userId == null
                ? Map.of()
                : progressService.getLessonProgressSummaryMap(userId, lessonIds);

        // Chuyển sang response kèm tiến độ học
        Page<LessonResponse> resultPage = lessonPage.map(lesson ->
                lessonMapper.toLessonResponse(lesson, progressMap.get(lesson.getId())));

        return PageResponse.of(resultPage);
    }

    @Override
    @Transactional(readOnly = true)
    public LessonDetailResponse getLessonById(Long userId, Long lessonId) {
        Lesson lesson = lessonAccessService.validateLessonAccess(userId, lessonId);
        return buildLessonDetail(userId, lesson);
    }

    @Override
    @Transactional(readOnly = true)
    public LessonDetailResponse getLessonBySlug(Long userId, String slug) {
        Lesson lesson = lessonAccessService.validateLessonAccess(userId, slug);
        return buildLessonDetail(userId, lesson);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonSegmentResponse> getSegmentsByLessonId(Long userId, Long lessonId) {
        Lesson lesson = lessonAccessService.validateLessonAccess(userId, lessonId);
        return buildSegments(userId, lesson);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonSegmentResponse> getSegmentsByLessonSlug(Long userId, String slug) {
        Lesson lesson = lessonAccessService.validateLessonAccess(userId, slug);
        return buildSegments(userId, lesson);
    }

    private LessonDetailResponse buildLessonDetail(Long userId, Lesson lesson) {
        long segmentCount = lessonSegmentRepository.countByLessonId(lesson.getId());

        LessonProgressSummaryResponse progress = userId == null
                ? null
                : progressService.findLessonProgressSummary(userId, lesson.getId()).orElse(null);

        return lessonMapper.toLessonDetailResponse(
                lesson,
                (int) segmentCount,
                progress,
                lessonProperties.dictationPassScore(),
                lessonProperties.shadowingPassScore()
        );
    }

    private List<LessonSegmentResponse> buildSegments(Long userId, Lesson lesson) {
        List<LessonSegment> segments = lessonSegmentRepository
                .findAllByLessonIdOrderByStartMsAsc(lesson.getId());

        if (segments.isEmpty()) {
            return List.of();
        }

        Map<Long, SegmentProgressResponse> progressBySegmentId =
                userId == null ? Map.of() : progressService.getSegmentProgressMap(userId, lesson.getId());

        return segments.stream()
                .map(segment -> lessonSegmentMapper.toLessonSegmentResponse(
                        segment,
                        progressBySegmentId.get(segment.getId())
                ))
                .toList();
    }
}
