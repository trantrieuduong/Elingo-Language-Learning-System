package com.elingo.learning.flashcard.dto.response;

public record CardLearningState(boolean learned, boolean starred, boolean hidden) {
    public static final CardLearningState NONE = new CardLearningState(false, false, false);

    public static CardLearningState of(Short srsLastGrade, Boolean starred, Boolean hidden) {
        return new CardLearningState(
                srsLastGrade != null,
                Boolean.TRUE.equals(starred),
                Boolean.TRUE.equals(hidden));
    }
}
