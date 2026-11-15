package com.elingo.report.service;

import com.elingo.report.entity.ReportTargetType;

public interface ReportChecker {

    ReportTargetType supports();

    /** throw AppException if user cant not report or resource not found */
    void check(Long targetId, Long reporterId);
}
