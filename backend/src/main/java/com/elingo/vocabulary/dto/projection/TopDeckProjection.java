package com.elingo.vocabulary.dto.projection;

public interface TopDeckProjection {
    Long getDeckId();
    String getTitle();
    String getCoverImageUrl();
    String getStatus();
    String getOwnerType();
    Boolean getIsPremium();
    Long getLearnerCount();
}
