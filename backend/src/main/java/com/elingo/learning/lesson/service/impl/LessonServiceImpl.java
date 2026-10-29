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
import com.elingo.learning.lesson.mapper.LessonProgressMapper;
import com.elingo.learning.lesson.mapper.LessonSegmentMapper;
import com.elingo.learning.lesson.repository.LessonRepository;
import com.elingo.learning.lesson.repository.LessonSegmentRepository;
import com.elingo.learning.lesson.repository.UserLessonProgressRepository;
import com.elingo.learning.lesson.repository.specification.LessonFilter;
import com.elingo.learning.lesson.repository.specification.LessonSpecifications;
import com.elingo.learning.lesson.service.LessonAccessService;
import com.elingo.learning.lesson.service.LessonProgressService;
import com.elingo.learning.lesson.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

    private static final int PAGE_SIZE = 9;

    private final LessonRepository lessonRepository;
    private final LessonSegmentRepository lessonSegmentRepository;
    private final UserLessonProgressRepository userLessonProgressRepository;
    private final LessonAccessService lessonAccessService;
    private final LessonProgressService lessonProgressService;
    private final LessonMapper lessonMapper;
    private final LessonProgressMapper lessonProgressMapper;
    private final LessonSegmentMapper lessonSegmentMapper;
    private final LessonProperties lessonProperties;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LessonResponse> getAllPublishedLessons(Long userId, GetPublishedLessonsRequest request) {
        // 1. Trim keyword, page mặc định = 1; cefrLevelIds/tagIds null hoặc rỗng = không lọc
        String keyword   = StringUtils.hasText(request.keyword()) ? request.keyword().trim() : null;
        int page = request.page() == null ? 1 : request.page();

        // 2. Paginate + sort: mới nhất trước
        Pageable pageable = PageRequest.of(page - 1, PAGE_SIZE,
                Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id")));
        Page<Lesson> lessonPage = lessonRepository.findAll(
                LessonSpecifications.withFilter(new LessonFilter(request.cefrLevelIds(), request.tagIds(), keyword)),
                pageable);

        // 3. Load tiến độ cho tất cả lesson trong trang — 1 query
        List<Long> lessonIds = lessonPage.getContent().stream().map(Lesson::getId).toList();
        Map<Long, LessonProgressSummaryResponse> progressMap = Map.of();
        if (userId != null && !lessonIds.isEmpty()) {
            progressMap = userLessonProgressRepository
                    .findAllByUserIdAndLessonIdIn(userId, lessonIds)
                    .stream()
                    .collect(Collectors.toMap(
                            p -> p.getLesson().getId(),
                            lessonProgressMapper::toLessonProgressSummaryResponse
                    ));
        }

        // 4. Map trong transaction; giữ reference để dùng trong lambda
        final Map<Long, LessonProgressSummaryResponse> finalProgressMap = progressMap;
        Page<LessonResponse> resultPage = lessonPage.map(lesson ->
                lessonMapper.toLessonResponse(lesson, finalProgressMap.get(lesson.getId())));

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

        LessonProgressSummaryResponse progress = userLessonProgressRepository
                .findByUserIdAndLessonId(userId, lesson.getId())
                .map(lessonProgressMapper::toLessonProgressSummaryResponse)
                .orElse(null);

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
                lessonProgressService.getSegmentProgressMap(userId, lesson.getId());

        return segments.stream()
                .map(segment -> lessonSegmentMapper.toLessonSegmentResponse(
                        segment,
                        progressBySegmentId.get(segment.getId())
                ))
                .toList();
    }
}
