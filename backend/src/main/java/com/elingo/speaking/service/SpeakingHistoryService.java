package com.elingo.speaking.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.speaking.dto.request.SpeakingRoomSearchRequest;
import com.elingo.speaking.dto.response.SpeakingHistoryItemResponse;
import com.elingo.speaking.dto.response.SpeakingHistorySummaryResponse;
import org.springframework.data.domain.Pageable;

public interface SpeakingHistoryService {
    PageResponse<SpeakingHistoryItemResponse> getHistory(Long userId, SpeakingRoomSearchRequest request, Pageable pageable);
    SpeakingHistorySummaryResponse getSummary(Long userId);
}
