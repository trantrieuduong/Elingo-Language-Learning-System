package com.elingo.learning.flashcard.dto.response;

public record CardLearningState(boolean learned, boolean starred, boolean hidden) {
    public static final CardLearningState NONE = new CardLearningState(false, false, false);
}
