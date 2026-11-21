package com.elingo.vocabulary.service.impl;

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
    public List<DeckRepository.StatusCountProjection> countDecksByStatus() {
        return deckRepository.countDecksByStatus();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeckRepository.OwnerTypeCountProjection> countDecksByOwnerType() {
        return deckRepository.countDecksByOwnerType();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeckRepository.PremiumCountProjection> countDecksByPremium() {
        return deckRepository.countDecksByPremium();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeckRepository.TopDeckProjection> getTopDecksByOwnerType(OwnerType ownerType, int limit) {
        return deckRepository.findTopDecksByOwnerType(ownerType.name(), Math.max(1, limit));
    }
}
