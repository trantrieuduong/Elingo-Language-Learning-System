package com.elingo.learning.lesson.service.impl;

import com.elingo.common.dto.PageResponse;
import com.elingo.common.enums.SystemFileModule;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.util.SlugUtils;
import com.elingo.file.util.MediaKeyResolver;
import com.elingo.learning.lesson.config.LessonProperties;
import com.elingo.learning.lesson.dto.projection.LessonSegmentCountProjection;
import com.elingo.learning.lesson.dto.request.AdminLessonSearchRequest;
import com.elingo.learning.lesson.dto.request.GetPublishedLessonsRequest;
import com.elingo.learning.lesson.dto.request.LessonRequest;
import com.elingo.learning.lesson.dto.response.AdminLessonResponse;
import com.elingo.learning.lesson.dto.response.LessonDetailResponse;
import com.elingo.learning.lesson.dto.response.LessonProgressSummaryResponse;
import com.elingo.learning.lesson.dto.response.LessonResponse;
import com.elingo.learning.lesson.dto.response.LessonSegmentResponse;
import com.elingo.learning.lesson.dto.response.SegmentProgressResponse;
import com.elingo.learning.lesson.entity.Lesson;
import com.elingo.learning.lesson.entity.LessonSegment;
import com.elingo.learning.lesson.entity.LessonStatus;
import com.elingo.learning.lesson.mapper.LessonMapper;
import com.elingo.learning.lesson.mapper.LessonSegmentMapper;
import com.elingo.learning.lesson.repository.LessonRepository;
import com.elingo.learning.lesson.repository.LessonSegmentRepository;
import com.elingo.learning.lesson.repository.specification.LessonFilter;
import com.elingo.learning.lesson.repository.specification.LessonSpecifications;
import com.elingo.learning.lesson.service.LessonAccessService;
import com.elingo.learning.lesson.service.LessonService;
import com.elingo.learning.lesson.util.YoutubeUrlUtils;
import com.elingo.progress.service.ProgressService;
import com.elingo.vocabulary.entity.CefrLevel;
import com.elingo.vocabulary.entity.Tag;
import com.elingo.vocabulary.repository.CefrLevelRepository;
import com.elingo.vocabulary.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final TagRepository tagRepository;
    private final CefrLevelRepository cefrLevelRepository;
    private final MediaKeyResolver mediaKeyResolver;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LessonResponse> getAllPublishedLessons(Long userId,
                                                               GetPublishedLessonsRequest request,
                                                               Pageable pageable) {
        // Trim keyword; cefrLevelIds/tagIds null hoặc rỗng = không lọc
        String keyword = StringUtils.hasText(request.keyword()) ? request.keyword().trim() : null;

        // Lấy danh sách bài học theo bộ lọc và phân trang
        Page<Lesson> lessonPage = lessonRepository.findAll(
                LessonSpecifications.withFilter(LessonFilter.forLearner(request.cefrLevelIds(), request.tagIds(), keyword)),
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

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminLessonResponse> searchLessonsForAdmin(AdminLessonSearchRequest request, Pageable pageable) {
        Page<Lesson> page = lessonRepository.findAll(
                LessonSpecifications.withFilter(LessonFilter.fromAdminRequest(request)), pageable);

        List<Long> ids = page.getContent().stream().map(Lesson::getId).toList();
        Map<Long, Long> countByLesson = ids.isEmpty()
                ? Map.of()
                : lessonSegmentRepository.countByLessonIds(ids).stream()
                    .collect(Collectors.toMap(LessonSegmentCountProjection::getLessonId,
                                              LessonSegmentCountProjection::getCountVal));

        return PageResponse.of(page.map(lesson ->
                lessonMapper.toAdminLessonResponse(lesson, countByLesson.getOrDefault(lesson.getId(), 0L).intValue())));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminLessonResponse getLessonByIdForAdmin(Long lessonId) {
        Lesson lesson = findLessonOrThrow(lessonId);
        return lessonMapper.toAdminLessonResponse(lesson, (int) lessonSegmentRepository.countByLessonId(lessonId));
    }

    @Override
    @Transactional
    public AdminLessonResponse createLesson(LessonRequest request) {
        if (!YoutubeUrlUtils.isValid(request.sourceUrl())) {
            throw new AppException(AppError.SOURCE_URL_INVALID);
        }
        var thumbnail = mediaKeyResolver.forCreate(request.thumbnailUrl(), SystemFileModule.LESSONS);

        Lesson lesson = lessonMapper.toLesson(request);
        lesson.setSlug(SlugUtils.generateUniqueSlug(request.title()));
        lesson.setStatus(LessonStatus.DRAFT);
        lesson.setPublishedAt(null);
        lesson.setThumbnailUrl(thumbnail.key());
        lesson.setTags(resolveTags(request.tagIds()));
        lesson.setCefrLevels(resolveCefrLevels(request.cefrLevelIds()));

        Lesson saved = lessonRepository.save(lesson);
        mediaKeyResolver.publish(thumbnail);

        return lessonMapper.toAdminLessonResponse(saved, 0);
    }

    @Override
    @Transactional
    public AdminLessonResponse updateLesson(Long lessonId, LessonRequest request) {
        Lesson lesson = findLessonOrThrow(lessonId);
        if (!YoutubeUrlUtils.isValid(request.sourceUrl())) {
            throw new AppException(AppError.SOURCE_URL_INVALID);
        }
        var thumbnail = mediaKeyResolver.forUpdate(request.thumbnailUrl(), lesson.getThumbnailUrl(), SystemFileModule.LESSONS);

        lessonSegmentRepository.findMaxEndMsByLessonId(lessonId).ifPresent(maxEnd -> {
            if (request.durationMs() < maxEnd) {
                throw new AppException(AppError.SEGMENT_EXCEEDS_DURATION);
            }
        });

        lessonMapper.updateLesson(lesson, request);
        lesson.setThumbnailUrl(thumbnail.key());
        lesson.setTags(resolveTags(request.tagIds()));
        lesson.setCefrLevels(resolveCefrLevels(request.cefrLevelIds()));

        mediaKeyResolver.publish(thumbnail);

        return lessonMapper.toAdminLessonResponse(lesson, (int) lessonSegmentRepository.countByLessonId(lessonId));
    }

    @Override
    @Transactional
    public AdminLessonResponse publishLesson(Long lessonId) {
        Lesson lesson = findLessonOrThrow(lessonId);
        long segmentCount = lessonSegmentRepository.countByLessonId(lessonId);
        if (segmentCount == 0) {
            throw new AppException(AppError.LESSON_PUBLISHED_FAILED);
        }
        if (lesson.getStatus() != LessonStatus.PUBLISHED) {
            lesson.setStatus(LessonStatus.PUBLISHED);
            lesson.setPublishedAt(LocalDateTime.now());
        }
        return lessonMapper.toAdminLessonResponse(lesson, (int) segmentCount);
    }

    @Override
    @Transactional
    public AdminLessonResponse unpublishLesson(Long lessonId) {
        Lesson lesson = findLessonOrThrow(lessonId);
        lesson.setStatus(LessonStatus.DRAFT);
        lesson.setPublishedAt(null);
        return lessonMapper.toAdminLessonResponse(lesson, (int) lessonSegmentRepository.countByLessonId(lessonId));
    }

    @Override
    @Transactional
    public AdminLessonResponse archiveLesson(Long lessonId) {
        Lesson lesson = findLessonOrThrow(lessonId);
        lesson.setStatus(LessonStatus.ARCHIVED); // publishedAt giữ nguyên
        return lessonMapper.toAdminLessonResponse(lesson, (int) lessonSegmentRepository.countByLessonId(lessonId));
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    private Lesson findLessonOrThrow(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new AppException(AppError.LESSON_NOT_FOUND));
    }

    private Set<Tag> resolveTags(Set<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Tag> tags = tagRepository.findAllById(tagIds);
        if (tags.size() != tagIds.size()) {
            throw new AppException(AppError.TAG_NOT_FOUND);
        }
        return new HashSet<>(tags);
    }

    private Set<CefrLevel> resolveCefrLevels(Set<Long> cefrLevelIds) {
        if (cefrLevelIds == null || cefrLevelIds.isEmpty()) {
            return new HashSet<>();
        }
        List<CefrLevel> cefrLevels = cefrLevelRepository.findAllById(cefrLevelIds);
        if (cefrLevels.size() != cefrLevelIds.size()) {
            throw new AppException(AppError.CEFR_LEVEL_NOT_FOUND);
        }
        return new HashSet<>(cefrLevels);
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
