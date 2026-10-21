package com.elingo.learning.dictation.service;

import com.elingo.learning.dictation.dto.request.UpdateDictationProgressRequest;
import com.elingo.learning.lesson.dto.response.SegmentSubmitResponse;

public interface DictationService {
    SegmentSubmitResponse updateProgress(Long userId, String slug, Long segmentId, UpdateDictationProgressRequest request);
}
