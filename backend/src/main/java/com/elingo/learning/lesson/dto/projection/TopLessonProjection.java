package com.elingo.learning.lesson.dto.projection;

public interface TopLessonProjection {
    Long getLessonId();
    String getTitle();
    String getThumbnailUrl();
    String getStatus();
    Boolean getIsPremium();
    Long getLearnerCount();
}
