package com.elingo.learning.lesson.service.impl;

import com.elingo.common.enums.LessonMode;
import com.elingo.learning.lesson.dto.response.SegmentProgressResponse;
import com.elingo.learning.lesson.dto.response.WordAccuracyResponse;
import com.elingo.learning.lesson.entity.Lesson;
import com.elingo.learning.lesson.entity.LessonSegment;
import com.elingo.learning.lesson.entity.ProgressStatus;
import com.elingo.learning.lesson.entity.SegmentWordAccuracy;
import com.elingo.learning.lesson.entity.UserLessonProgress;
import com.elingo.learning.lesson.entity.UserSegmentProgress;
import com.elingo.learning.lesson.mapper.LessonProgressMapper;
import com.elingo.learning.lesson.repository.LessonRepository;
import com.elingo.learning.lesson.repository.LessonSegmentRepository;
import com.elingo.learning.lesson.repository.SegmentWordAccuracyRepository;
import com.elingo.learning.lesson.repository.UserLessonProgressRepository;
import com.elingo.learning.lesson.repository.UserSegmentProgressRepository;
import com.elingo.learning.lesson.service.LessonProgressService;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "LESSON-PROGRESS-SERVICE")
public class LessonProgressServiceImpl implements LessonProgressService {

    private final UserLessonProgressRepository userLessonProgressRepository;
    private final UserSegmentProgressRepository userSegmentProgressRepository;
    private final SegmentWordAccuracyRepository segmentWordAccuracyRepository;
    private final LessonSegmentRepository lessonSegmentRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressMapper lessonProgressMapper;

    @Override
    @Transactional(readOnly = true)
    public SegmentProgressResponse buildSegmentProgress(UserSegmentProgress progress) {
        List<WordAccuracyResponse> words = segmentWordAccuracyRepository
                .findAllByUserSegmentProgressIdOrderByIdAsc(progress.getId())
                .stream()
                .map(lessonProgressMapper::toWordAccuracyResponse)
                .toList();
        return lessonProgressMapper.toSegmentProgressResponse(progress, words);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, SegmentProgressResponse> getSegmentProgressMap(Long userId, Long lessonId) {
        List<UserSegmentProgress> progressList = userSegmentProgressRepository.findAllByUserIdAndLessonId(userId, lessonId);
        if (progressList.isEmpty()) {
            return Map.of();
        }

        List<Long> progressIds = progressList.stream().map(UserSegmentProgress::getId).toList();
        Map<Long, List<WordAccuracyResponse>> wordsByProgressId = segmentWordAccuracyRepository
                .findAllByUserSegmentProgressIdInOrderByIdAsc(progressIds)
                .stream()
                .collect(Collectors.groupingBy(
                        w -> w.getUserSegmentProgress().getId(),
                        Collectors.mapping(lessonProgressMapper::toWordAccuracyResponse, Collectors.toList())
                ));

        return progressList.stream()
                .collect(Collectors.toMap(
                        p -> p.getSegment().getId(),
                        p -> lessonProgressMapper.toSegmentProgressResponse(p, wordsByProgressId.getOrDefault(p.getId(), List.of()))
                ));
    }

    @Override
    @Transactional
    public UserSegmentProgress getOrCreateSegmentProgress(Long userId, LessonSegment segment) {
        return userSegmentProgressRepository.findByUserIdAndSegmentId(userId, segment.getId())
                .orElseGet(() -> {
                    User userRef = userRepository.getReferenceById(userId);
                    Lesson lessonRef = segment.getLesson();
                    UserSegmentProgress created = userSegmentProgressRepository.save(
                            UserSegmentProgress.builder()
                                    .user(userRef)
                                    .lesson(lessonRef)
                                    .segment(segment)
                                    .build()
                    );
                    log.info("Segment progress created userId={} segmentId={}", userId, segment.getId());
                    return created;
                });
    }

    @Override
    @Transactional
    public UserLessonProgress recordSegmentSubmitted(Long userId, Long lessonId, LessonMode mode, Integer segmentStartMs) {
        long total = lessonSegmentRepository.countByLessonId(lessonId);

        long done = (mode == LessonMode.DICTATION)
                ? userSegmentProgressRepository.countByUserIdAndLessonIdAndDictationAttemptCountGreaterThan(userId, lessonId, 0)
                : userSegmentProgressRepository.countByUserIdAndLessonIdAndShadowingAttemptCountGreaterThan(userId, lessonId, 0);

        BigDecimal pct = BigDecimal.valueOf(done * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
        ProgressStatus newStatus = done >= total ? ProgressStatus.COMPLETED : ProgressStatus.IN_PROGRESS;

        UserLessonProgress progress = userLessonProgressRepository.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> {
                    User userRef = userRepository.getReferenceById(userId);
                    Lesson lessonRef = lessonRepository.getReferenceById(lessonId);
                    return UserLessonProgress.builder()
                            .user(userRef)
                            .lesson(lessonRef)
                            .build();
                });

        if (mode == LessonMode.DICTATION) {
            progress.setDictationStatus(newStatus);
            progress.setDictationProgressPct(pct);
            progress.setDictationLastStartMs(segmentStartMs);
        } else {
            progress.setShadowingStatus(newStatus);
            progress.setShadowingProgressPct(pct);
            progress.setShadowingLastStartMs(segmentStartMs);
        }

        UserLessonProgress saved = userLessonProgressRepository.save(progress);
        log.info("Lesson progress updated userId={} lessonId={} mode={} pct={} status={}",
                userId, lessonId, mode, pct, newStatus);
        return saved;
    }

    @Override
    @Transactional
    public void replaceWordsAccuracy(UserSegmentProgress progress, List<SegmentWordAccuracy> wordsAccuracy) {
        segmentWordAccuracyRepository.deleteAllByUserSegmentProgressId(progress.getId());
        if (!wordsAccuracy.isEmpty()) {
            segmentWordAccuracyRepository.saveAll(wordsAccuracy);
        }
    }
}
