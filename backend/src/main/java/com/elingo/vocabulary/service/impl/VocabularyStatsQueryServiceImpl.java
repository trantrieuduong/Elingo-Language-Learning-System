package com.elingo.vocabulary.service.impl;

import com.elingo.vocabulary.dto.projection.OwnerTypeCountProjection;
import com.elingo.vocabulary.dto.projection.PremiumCountProjection;
import com.elingo.vocabulary.dto.projection.StatusCountProjection;
import com.elingo.vocabulary.dto.projection.TopDeckProjection;
import com.elingo.vocabulary.entity.OwnerType;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.service.VocabularyStatsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VocabularyStatsQueryServiceImpl implements VocabularyStatsQueryService {

    private final DeckRepository deckRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StatusCountProjection> countDecksByStatus() {
        return deckRepository.countDecksByStatus();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OwnerTypeCountProjection> countDecksByOwnerType() {
        return deckRepository.countDecksByOwnerType();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PremiumCountProjection> countDecksByPremium() {
        return deckRepository.countDecksByPremium();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopDeckProjection> getTopDecksByOwnerType(OwnerType ownerType, int limit) {
        return deckRepository.findTopDecksByOwnerType(ownerType.name(), Math.max(1, limit));
    }
}
