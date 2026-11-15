package com.elingo.report.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.report.entity.ReportTargetType;
import com.elingo.report.service.ReportChecker;
import com.elingo.vocabulary.entity.OwnerType;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.service.DeckAccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class DeckReportChecker implements ReportChecker {

    private final DeckRepository deckRepository;
    private final DeckAccessPolicy deckAccessPolicy;

    @Override
    public ReportTargetType supports() {
        return ReportTargetType.DECK;
    }

    @Override
    public void check(Long targetId, Long reporterId) {
        DeckRepository.DeckAccessView deck = deckRepository.findAccessViewById(targetId)
                .orElseThrow(() -> new AppException(AppError.DECK_NOT_FOUND));

        deckAccessPolicy.assertCanView(deck, reporterId);

        if (deck.getOwnerType() == OwnerType.USER && Objects.equals(deck.getOwnerId(), reporterId))
            throw new AppException(AppError.CANNOT_REPORT_SELF);
    }
}
