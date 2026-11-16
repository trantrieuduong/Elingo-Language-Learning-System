package com.elingo.speaking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record IceServerResponse(
        List<String> urls,
        String username,
        String credential
) {
}
