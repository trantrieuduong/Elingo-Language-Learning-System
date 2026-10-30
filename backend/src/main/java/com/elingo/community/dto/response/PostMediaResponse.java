package com.elingo.community.dto.response;

import com.elingo.common.annotation.BuildMediaUrl;
import com.elingo.community.entity.MediaType;

public record PostMediaResponse(
        Integer order,
        MediaType mediaType,
        @BuildMediaUrl String mediaUrl
) {
}
