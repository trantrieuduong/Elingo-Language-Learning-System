package com.elingo.learning.flashcard.service.impl;

import com.elingo.config.FlashcardProperties;
import com.elingo.learning.flashcard.entity.UserCardState;
import com.elingo.learning.flashcard.service.SpacedRepetitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "SRS-SERVICE")
public class SpacedRepetitionServiceImpl implements SpacedRepetitionService {
    private final FlashcardProperties flashcardProperties;

    // grade: 0=Again, 1=Hard, 2=Good, 3=Easy
    // EF'=EF+(0.1-(3-q)*(0.08+(3-q)*0.02))
    // Tham khảo: https://super-memory.com/english/ol/sm2.htm
    @Override
    public void calculateNextSRS(UserCardState state, int grade) {
        log.info("Calculate next SRS for userId={}, cardId={}, grade={}",
                state.getUser().getId(), state.getCard().getId(), grade);

        FlashcardProperties.Srs srs = flashcardProperties.getSrs();
        BigDecimal ef = state.getSrsEaseFactor();
        int prevInterval = state.getSrsInterval();

        BigDecimal newEf = switch (grade) {
            case 0 -> ef.subtract(srs.getAgainEfPenalty()).max(srs.getEfMin());
            case 1 -> ef.subtract(srs.getHardEfPenalty()).max(srs.getEfMin());
            case 2 -> ef;
            default -> ef.add(srs.getEasyEfBonus());
        };
        newEf = newEf.setScale(2, RoundingMode.HALF_UP);

        int newInterval = switch (grade) {
            case 0 -> 0;
            case 1 -> (prevInterval == 0) ? 1
                    : BigDecimal.valueOf(prevInterval).multiply(srs.getHardIntervalFactor()).setScale(0, RoundingMode.HALF_UP).intValue();
            case 2 -> (prevInterval == 0) ? 3
                    : BigDecimal.valueOf(prevInterval).multiply(ef).setScale(0, RoundingMode.HALF_UP).intValue();
            default -> (prevInterval == 0) ? 5
                    : BigDecimal.valueOf(prevInterval).multiply(newEf).multiply(srs.getEasyIntervalFactor()).setScale(0, RoundingMode.HALF_UP).intValue();
        };

        LocalDateTime nextReviewAt;
        if (newInterval == 0) {
            nextReviewAt = LocalDateTime.now().plusMinutes(srs.getAgainReviewMinutes());
        } else {
            nextReviewAt = LocalDateTime.now().plusDays(newInterval);
        }

        state.setSrsEaseFactor(newEf);
        state.setSrsInterval(newInterval);
        state.setSrsLastGrade((short) grade);
        state.setSrsNextReviewAt(nextReviewAt);

        log.info("Calculate next SRS successfully for userId={}, cardId={}, grade={}, ef={}->{}, interval={}->{}, next={}",
                state.getUser().getId(), state.getCard().getId(),
                grade, ef, newEf, prevInterval, newInterval, nextReviewAt);
    }
}