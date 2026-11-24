package com.elingo.learning.lesson.dto.projection;

import com.elingo.learning.lesson.entity.LessonStatus;

public interface LessonStatusCountProjection {
    LessonStatus getStatus();
    Long getCountVal();
}
