package com.elingo.vocabulary.dto.projection;

import com.elingo.vocabulary.entity.DeckStatus;

public interface StatusCountProjection {
    DeckStatus getStatus();
    Long getCountVal();
}
