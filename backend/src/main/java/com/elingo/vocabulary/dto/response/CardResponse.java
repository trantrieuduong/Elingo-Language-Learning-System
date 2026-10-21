package com.elingo.vocabulary.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)//chỉ trả về các field non null
public record CardResponse(
    Long id,
    Integer order,
    String term,
    String pos,
    String translation,
    String explanationVi,
    String explanationEn,
    String examplesVi,
    String examplesEn,
    String imageUrl,
    List<CardPhoneticResponse> phonetics,
    Boolean flagsStarred
) {}
