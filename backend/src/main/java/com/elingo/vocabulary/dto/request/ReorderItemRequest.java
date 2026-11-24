package com.elingo.vocabulary.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReorderItemRequest(
        @NotEmpty(message = "INVALID_REQUEST")
        List<Long> orderedIds
) {
    public ReorderItemRequest {
        orderedIds = orderedIds == null ? List.of() : List.copyOf(orderedIds);
    }
}
