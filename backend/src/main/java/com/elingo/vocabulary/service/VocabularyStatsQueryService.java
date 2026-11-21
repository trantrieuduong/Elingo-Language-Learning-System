package com.elingo.vocabulary.service;

import com.elingo.vocabulary.entity.OwnerType;
import com.elingo.vocabulary.repository.DeckRepository;

import java.util.List;

public interface VocabularyStatsQueryService {
    List<DeckRepository.StatusCountProjection> countDecksByStatus();
    List<DeckRepository.OwnerTypeCountProjection> countDecksByOwnerType();
    List<DeckRepository.PremiumCountProjection> countDecksByPremium();
    List<DeckRepository.TopDeckProjection> getTopDecksByOwnerType(OwnerType ownerType, int limit);
}
