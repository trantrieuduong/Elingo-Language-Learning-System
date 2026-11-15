package com.elingo.report.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.community.repository.PostRepository;
import com.elingo.report.entity.ReportTargetType;
import com.elingo.report.service.ReportChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class PostReportChecker implements ReportChecker {

    private final PostRepository postRepository;

    @Override
    public ReportTargetType supports() {
        return ReportTargetType.POST;
    }

    @Override
    public void check(Long targetId, Long reporterId) {
        Long ownerId = postRepository.findPublishedPostOwnerId(targetId)
                .orElseThrow(() -> new AppException(AppError.POST_NOT_FOUND));

        if (Objects.equals(reporterId, ownerId))
            throw new AppException(AppError.CANNOT_REPORT_SELF);
    }
}
