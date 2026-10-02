package com.elingo.vocabulary.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GetPublishedDecksRequest(

        @Pattern(
                regexp = "^(A1|A2|B1|B2|C1|C2)$",
                message = "CEFR level code must be one of: A1, A2, B1, B2, C1, C2"
        )
        String cefrCode,//hard code message, hard code cefrCode

        @Size(max = 100, message = "Tag code must not exceed 100 characters")
        String tagCode,//hard code message

        @Size(max = 255, message = "Keyword must not exceed 255 characters")
        String keyword,//hard code message

        @Min(value = 0, message = "Page index must be 0 or greater")
        Integer page//hard code message
) {

    public GetPublishedDecksRequest {
        if (page == null) page = 0;
    }
}
