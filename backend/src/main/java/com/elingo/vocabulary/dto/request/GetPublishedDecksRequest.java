package com.elingo.vocabulary.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record GetPublishedDecksRequest(
        @Size(max = 10, message = "CEFR_LEVEL_CODE_INVALID")
        String cefrCode,

        @Size(max = 100, message = "TAG_INVALID")
        String tagCode,

        @Size(max = 255, message = "KEYWORD_INVALID")
        String keyword,

        @Min(value = 0, message = "PAGE_INDEX_INVALID")
        Integer page) {

    public GetPublishedDecksRequest {
        if (page == null) page = 0;
    }
}
