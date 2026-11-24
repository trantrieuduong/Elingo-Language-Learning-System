package com.elingo.vocabulary.dto.request;

import com.elingo.vocabulary.entity.DeckStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDeckStatusRequest(
        @NotNull(message = "INVALID_REQUEST")
        DeckStatus status
) {
}
