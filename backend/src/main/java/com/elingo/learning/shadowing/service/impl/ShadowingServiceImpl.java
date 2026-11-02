package com.elingo.learning.shadowing.service.impl;

import com.elingo.common.enums.LessonMode;
import com.elingo.common.event.FileAttachedEvent;
import com.elingo.common.event.FileDeletedEvent;
import com.elingo.common.event.XpGainedEvent;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.file.util.FileKey;
import com.elingo.gamification.entity.XpSource;
import com.elingo.learning.lesson.config.LessonProperties;
import com.elingo.learning.lesson.dto.response.LessonProgressSummaryResponse;
import com.elingo.learning.lesson.entity.Lesson;
import com.elingo.learning.lesson.entity.LessonSegment;
import com.elingo.learning.lesson.entity.SegmentWordAccuracy;
import com.elingo.learning.lesson.entity.UserLessonProgress;
import com.elingo.learning.lesson.entity.UserSegmentProgress;
import com.elingo.learning.lesson.mapper.LessonProgressMapper;
import com.elingo.learning.lesson.repository.LessonSegmentRepository;
import com.elingo.learning.lesson.service.LessonAccessService;
import com.elingo.learning.lesson.service.LessonProgressService;
import com.elingo.learning.lesson.util.TranscriptUtils;
import com.elingo.learning.lesson.dto.response.SegmentSubmitResponse;
import com.elingo.learning.shadowing.dto.request.UpdateShadowingProgressRequest;
import com.elingo.learning.shadowing.dto.request.WordAccuracyItem;
import com.elingo.learning.shadowing.service.ShadowingService;
import com.elingo.learning.shadowing.service.ShadowingScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "SHADOWING-SERVICE")
public class ShadowingServiceImpl implements ShadowingService {

    private final LessonAccessService lessonAccessService;
    private final LessonSegmentRepository lessonSegmentRepository;
    private final LessonProgressService lessonProgressService;
    private final LessonProgressMapper lessonProgressMapper;
    private final ShadowingScoringService scoringService;
    private final LessonProperties lessonProperties;
    private final ApplicationEventPublisher publisher;

    @Override
    @Transactional
    public SegmentSubmitResponse updateProgress(Long userId, Long lessonId, Long segmentId, UpdateShadowingProgressRequest request) {
        // 1. Kiểm tra quyền truy cập
        Lesson lesson = lessonAccessService.validateLessonAccess(userId, lessonId);

        // 2. Tìm segment thuộc đúng lesson
        LessonSegment segment = lessonSegmentRepository.findByIdAndLessonId(segmentId, lesson.getId())
                .orElseThrow(() -> new AppException(AppError.LESSON_SEGMENT_NOT_FOUND));

        // 3. Kiểm tra mọi thứ trước khi ghi
        // 3a. Số lượng từ không vượt quá giới hạn theo transcript của segment
        int totalWords = TranscriptUtils.countWords(segment.getTranscriptNormalized());
        List<WordAccuracyItem> wordItems = request.wordsAccuracy();
        if (wordItems.size() > Math.max(totalWords * 2, 20)) {
            throw new AppException(AppError.WORDS_ACCURACY_INVALID);
        }

        // 3b. Audio: key phải thuộc chính user này
        String audioKey = request.audioUrl();
        requireOwned(audioKey, userId);
        String newUploadsKey = FileKey.toUploads(audioKey); // sai vùng -> AppException(INVALID_REQUEST)

        // 4. Tính finalScore
        int finalScore = scoringService.calculateFinalScore(request.score(), request.attemptCount());

        // 5. Lấy / tạo UserSegmentProgress
        UserSegmentProgress progress = lessonProgressService.getOrCreateSegmentProgress(userId, segment);
        BigDecimal previousBest = progress.getShadowingBestScore();
        String oldAudioKey = progress.getShadowingLatestAudioUrl();

        // 6. Ghi attemptCount & bestScore
        progress.setShadowingAttemptCount(Math.max(progress.getShadowingAttemptCount(), request.attemptCount()));

        BigDecimal finalScoreDecimal = BigDecimal.valueOf(finalScore).setScale(2, RoundingMode.HALF_UP);
        if (finalScoreDecimal.compareTo(previousBest) > 0) {
            progress.setShadowingBestScore(finalScoreDecimal);
        }

        // 7. Thay toàn bộ word accuracy: build list từ wordItems rồi gọi service
        List<SegmentWordAccuracy> accuraciesToSave = new ArrayList<>(wordItems.size());
        for (WordAccuracyItem item : wordItems) {
            accuraciesToSave.add(SegmentWordAccuracy.builder()
                    .userSegmentProgress(progress)
                    .word(item.word().trim())
                    .accuracy(item.accuracy().setScale(2, RoundingMode.HALF_UP))
                    .build());
        }
        lessonProgressService.replaceWordsAccuracy(progress, accuraciesToSave);

        // 8. Xử lý audio file promotion & event
        if (!newUploadsKey.equals(oldAudioKey)) {
            progress.setShadowingLatestAudioUrl(newUploadsKey);
            publisher.publishEvent(new FileAttachedEvent(audioKey, newUploadsKey));
            if (FileKey.isInZone(oldAudioKey, FileKey.UPLOADS_PREFIX)) {
                publisher.publishEvent(new FileDeletedEvent(List.of(oldAudioKey)));
            }
        }

        // 9. Cập nhật UserLessonProgress
        UserLessonProgress lessonProgress = lessonProgressService.recordSegmentSubmitted(
                userId, lesson.getId(), LessonMode.SHADOWING, segment.getStartMs());

        // 10. Lần đầu đạt ngưỡng -> phát XP segment
        int pass = lessonProperties.shadowingPassScore();
        if (finalScore >= pass && previousBest.compareTo(BigDecimal.valueOf(pass)) < 0) {
            log.info("Shadowing segment passed lessonId={} segmentId={} score={}", lesson.getId(), segmentId, finalScore);
            publisher.publishEvent(new XpGainedEvent(userId, segmentId, XpSource.SEGMENT_SHADOWING, LocalDateTime.now()));
        }

        log.info("Shadowing progress updated lessonId={} segmentId={} score={} bestScore={}",
                lesson.getId(), segmentId, finalScore, progress.getShadowingBestScore());

        // 11. Dựng response trong transaction
        LessonProgressSummaryResponse summaryResponse = lessonProgressMapper.toLessonProgressSummaryResponse(lessonProgress);
        boolean passed = finalScore >= pass;

        return new SegmentSubmitResponse(finalScore, passed, lessonProgressService.buildSegmentProgress(progress), summaryResponse);
    }

    // Key phải thuộc chính user này
    private void requireOwned(String key, Long userId) {
        if (!FileKey.isOwnedBy(key, userId)) {
            throw new AppException(AppError.FILE_ACCESS_DENIED);
        }
    }
}
