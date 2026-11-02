package com.elingo.learning.dictation.service.impl;

import com.elingo.common.enums.LessonMode;
import com.elingo.common.event.XpGainedEvent;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.gamification.entity.XpSource;
import com.elingo.learning.dictation.dto.request.UpdateDictationProgressRequest;
import com.elingo.learning.dictation.service.DictationScoringService;
import com.elingo.learning.dictation.service.DictationService;
import com.elingo.learning.lesson.config.LessonProperties;
import com.elingo.learning.lesson.dto.response.LessonProgressSummaryResponse;
import com.elingo.learning.lesson.dto.response.SegmentProgressResponse;
import com.elingo.learning.lesson.dto.response.SegmentSubmitResponse;
import com.elingo.learning.lesson.entity.Lesson;
import com.elingo.learning.lesson.entity.LessonSegment;
import com.elingo.learning.lesson.entity.UserLessonProgress;
import com.elingo.learning.lesson.entity.UserSegmentProgress;
import com.elingo.learning.lesson.mapper.LessonProgressMapper;
import com.elingo.learning.lesson.repository.LessonSegmentRepository;
import com.elingo.learning.lesson.service.LessonAccessService;
import com.elingo.learning.lesson.service.LessonProgressService;
import com.elingo.learning.lesson.util.TranscriptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "DICTATION-SERVICE")
public class DictationServiceImpl implements DictationService {

    private final LessonAccessService lessonAccessService;
    private final LessonSegmentRepository lessonSegmentRepository;
    private final LessonProgressService lessonProgressService;
    private final DictationScoringService scoringService;
    private final LessonProgressMapper lessonProgressMapper;
    private final LessonProperties lessonProperties;
    private final ApplicationEventPublisher publisher;

    @Override
    @Transactional
    public SegmentSubmitResponse updateProgress(Long userId, Long lessonId, Long segmentId, UpdateDictationProgressRequest request) {
        // 1. Kiểm tra quyền truy cập
        Lesson lesson = lessonAccessService.validateLessonAccess(userId, lessonId);

        // 2. Tìm segment thuộc đúng lesson
        LessonSegment segment = lessonSegmentRepository.findByIdAndLessonId(segmentId, lesson.getId())
                .orElseThrow(() -> new AppException(AppError.LESSON_SEGMENT_NOT_FOUND));

        // 3. Đếm từ transcript chuẩn hoá & kẹp hint
        int totalWords = TranscriptUtils.countWords(segment.getTranscriptNormalized());
        int hint = Math.min(request.hintUsedCount(), totalWords);

        // 4. Lấy / tạo UserSegmentProgress và lấy điểm best cũ
        UserSegmentProgress progress = lessonProgressService.getOrCreateSegmentProgress(userId, segment);
        BigDecimal previousBest = progress.getDictationBestScore();

        // 5. Tính điểm lần nộp này
        int score = scoringService.calculateScore(request.attemptCount(), hint, totalWords);

        // 6. Ghi vào progress: attemptCount = max(cũ, request)
        boolean isFirstAttempt = progress.getDictationAttemptCount() == 0;
        progress.setDictationAttemptCount(Math.max(progress.getDictationAttemptCount(), request.attemptCount()));

        // 7. Ghi bestScore & hintUsedCount khi điểm lần này > bestScore cũ hoặc là lần nộp đầu tiên
        BigDecimal scoreDecimal = BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
        if (scoreDecimal.compareTo(previousBest) > 0 || isFirstAttempt) {
            progress.setDictationBestScore(scoreDecimal);
            progress.setDictationHintUsedCount(hint);
        }

        // 8. Cập nhật UserLessonProgress
        UserLessonProgress lessonProgress = lessonProgressService.recordSegmentSubmitted(
                userId, lesson.getId(), LessonMode.DICTATION, segment.getStartMs());

        // 9. Lần đầu đạt ngưỡng (phát XP segment)
        int pass = lessonProperties.dictationPassScore();
        if (score >= pass && previousBest.compareTo(BigDecimal.valueOf(pass)) < 0) {
            log.info("Dictation segment passed lessonId={} segmentId={} score={}", lesson.getId(), segmentId, score);
            publisher.publishEvent(new XpGainedEvent(userId, segmentId, XpSource.SEGMENT_DICTATION, LocalDateTime.now()));
        }

        log.info("Dictation progress updated lessonId={} segmentId={} score={} bestScore={}",
                lesson.getId(), segmentId, score, progress.getDictationBestScore());

        // 10. Dựng response (Dictation không có word accuracy)
        SegmentProgressResponse segmentResponse = lessonProgressMapper.toSegmentProgressResponse(progress, List.of());
        LessonProgressSummaryResponse summaryResponse = lessonProgressMapper.toLessonProgressSummaryResponse(lessonProgress);
        boolean passed = score >= pass;

        return new SegmentSubmitResponse(score, passed, segmentResponse, summaryResponse);
    }
}
