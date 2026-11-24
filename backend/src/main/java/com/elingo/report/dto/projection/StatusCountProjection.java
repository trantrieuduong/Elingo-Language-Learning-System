package com.elingo.report.dto.projection;

import com.elingo.report.entity.ReportStatus;

public interface StatusCountProjection {
    ReportStatus getStatus();
    Long getCountVal();
}
