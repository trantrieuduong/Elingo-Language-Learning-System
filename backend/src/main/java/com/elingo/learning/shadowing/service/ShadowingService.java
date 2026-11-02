package com.elingo.learning.shadowing.service;

import com.elingo.learning.lesson.dto.response.SegmentSubmitResponse;
import com.elingo.learning.shadowing.dto.request.UpdateShadowingProgressRequest;

public interface ShadowingService {
    SegmentSubmitResponse updateProgress(Long userId, Long lessonId, Long segmentId, UpdateShadowingProgressRequest request);
}
