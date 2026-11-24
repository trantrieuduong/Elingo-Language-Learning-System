package com.elingo.community.dto.projection;

public interface PostBacklogProjection {
    Long getPendingCount();
    Long getNeedsReviewCount();
}
