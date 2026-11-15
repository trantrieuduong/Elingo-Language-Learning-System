package com.elingo.report.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.report.entity.ReportTargetType;
import com.elingo.report.service.ReportChecker;
import com.elingo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class UserReportChecker implements ReportChecker {

    private final UserRepository userRepository;

    @Override
    public ReportTargetType supports() {
        return ReportTargetType.USER;
    }

    @Override
    public void check(Long targetId, Long reporterId) {
        if (Objects.equals(reporterId, targetId))
            throw new AppException(AppError.CANNOT_REPORT_SELF);

        if (!userRepository.existsById(targetId))
            throw new AppException(AppError.USER_NOT_FOUND);
    }
}
