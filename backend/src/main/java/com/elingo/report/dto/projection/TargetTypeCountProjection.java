package com.elingo.report.dto.projection;

import com.elingo.report.entity.ReportTargetType;

public interface TargetTypeCountProjection {
    ReportTargetType getTargetType();
    Long getCountVal();
}
