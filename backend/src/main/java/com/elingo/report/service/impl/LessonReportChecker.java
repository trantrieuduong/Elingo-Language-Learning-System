package com.elingo.report.service.impl;

import com.elingo.learning.lesson.service.LessonAccessService;
import com.elingo.report.entity.ReportTargetType;
import com.elingo.report.service.ReportChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LessonReportChecker implements ReportChecker {

    private final LessonAccessService lessonAccessService;

    @Override
    public ReportTargetType supports() {
        return ReportTargetType.LESSON;
    }

    @Override
    public void check(Long targetId, Long reporterId) {
        lessonAccessService.validateLessonForReport(targetId);
    }
}
