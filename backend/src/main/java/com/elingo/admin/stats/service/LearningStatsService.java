package com.elingo.admin.stats.service;

import com.elingo.admin.stats.dto.request.StatsDateRangeRequest;
import com.elingo.admin.stats.dto.response.LearningStatsResponses;

public interface LearningStatsService {
    LearningStatsResponses.LearningActivityResponse getLearningActivity(StatsDateRangeRequest request);
    LearningStatsResponses.DeckStatsResponse getDeckStats(int limit);
    LearningStatsResponses.LessonStatsResponse getLessonStats(int limit);
}
