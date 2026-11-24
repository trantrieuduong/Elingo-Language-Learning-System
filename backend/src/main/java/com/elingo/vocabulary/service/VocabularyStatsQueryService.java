package com.elingo.vocabulary.service;

import com.elingo.vocabulary.dto.projection.OwnerTypeCountProjection;
import com.elingo.vocabulary.dto.projection.PremiumCountProjection;
import com.elingo.vocabulary.dto.projection.StatusCountProjection;
import com.elingo.vocabulary.dto.projection.TopDeckProjection;
import com.elingo.vocabulary.entity.OwnerType;

import java.util.List;

public interface VocabularyStatsQueryService {
    List<StatusCountProjection> countDecksByStatus();
    List<OwnerTypeCountProjection> countDecksByOwnerType();
    List<PremiumCountProjection> countDecksByPremium();
    List<TopDeckProjection> getTopDecksByOwnerType(OwnerType ownerType, int limit);
}
