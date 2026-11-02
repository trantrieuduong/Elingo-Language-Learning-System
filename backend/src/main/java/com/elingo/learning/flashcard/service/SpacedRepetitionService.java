package com.elingo.learning.flashcard.service;

import com.elingo.learning.flashcard.entity.UserCardState;

public interface SpacedRepetitionService {
    void calculateNextSRS(UserCardState state, int grade);
}