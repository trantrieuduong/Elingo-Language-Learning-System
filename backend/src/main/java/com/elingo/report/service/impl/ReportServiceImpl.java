package com.elingo.report.service.impl;

import com.elingo.common.dto.PageResponse;
import com.elingo.common.enums.MediaKind;
import com.elingo.common.event.FileAttachedEvent;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.file.util.FileKey;
import com.elingo.report.dto.request.CreateReportRequest;
import com.elingo.report.dto.response.ReportResponse;
import com.elingo.report.entity.Report;
import com.elingo.report.entity.ReportImage;
import com.elingo.report.entity.ReportStatus;
import com.elingo.report.entity.ReportTargetType;
import com.elingo.report.mapper.ReportMapper;
import com.elingo.report.repository.ReportImageRepository;
import com.elingo.report.repository.ReportRepository;
import com.elingo.report.service.ReportChecker;
import com.elingo.report.service.ReportService;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j(topic = "REPORT-SERVICE")
@Service
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final ReportImageRepository reportImageRepository;
    private final UserRepository userRepository;
    private final ReportMapper reportMapper;
    private final ApplicationEventPublisher publisher;
    private final Map<ReportTargetType, ReportChecker> checkers;

    public ReportServiceImpl(ReportRepository reportRepository,
                             ReportImageRepository reportImageRepository,
                             UserRepository userRepository,
                             ReportMapper reportMapper,
                             ApplicationEventPublisher publisher,
                             List<ReportChecker> checkerList) {
        this.reportRepository = reportRepository;
        this.reportImageRepository = reportImageRepository;
        this.userRepository = userRepository;
        this.reportMapper = reportMapper;
        this.publisher = publisher;

        this.checkers = new EnumMap<>(ReportTargetType.class);
        checkerList.forEach(c -> this.checkers.put(c.supports(), c));
        for (ReportTargetType type : ReportTargetType.values()) {
            if (!checkers.containsKey(type)) {
                throw new IllegalStateException("Missing ReportChecker for " + type);
            }
        }
    }

    @Override
    @Transactional
    public ReportResponse create(Long userId, CreateReportRequest request) {
        User reporter = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        ReportChecker checker = checkers.get(request.targetType());
        if (checker == null)
            throw new AppException(AppError.INVALID_REQUEST);
        checker.check(request.targetId(), userId);

        boolean alreadyReported = reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                userId, request.targetType(), request.targetId(), ReportStatus.PENDING);
        if (alreadyReported) {
            throw new AppException(AppError.REPORT_ALREADY_EXISTS);
        }

        List<String> verifiedKeys = request.imageUrls() != null
                ? request.imageUrls().stream().distinct().toList()
                : List.of();
        List<String> uploadsKeys = new ArrayList<>(verifiedKeys.size());
        for (String verifiedKey : verifiedKeys) {
            requireOwned(verifiedKey, userId);
            String uploadsKey = requireMediaKind(verifiedKey, MediaKind.IMAGE);
            uploadsKeys.add(uploadsKey);
        }

        Report report = Report.builder()
                .reporter(reporter)
                .targetType(request.targetType())
                .targetId(request.targetId())
                .description(request.description())
                .status(ReportStatus.PENDING)
                .build();
        Report savedReport = reportRepository.save(report);

        List<ReportImage> reportImages = new ArrayList<>(uploadsKeys.size());
        for (int i = 0; i < verifiedKeys.size(); i++) {
            String verifiedKey = verifiedKeys.get(i);
            String uploadsKey = uploadsKeys.get(i);
            ReportImage image = ReportImage.builder()
                    .report(savedReport)
                    .imageUrl(uploadsKey)
                    .build();
            reportImages.add(image);
            publisher.publishEvent(new FileAttachedEvent(verifiedKey, uploadsKey));
        }

        if (!reportImages.isEmpty())
            reportImageRepository.saveAll(reportImages);

        log.info("Report created reportId={} targetType={} targetId={} imageCount={}",
                savedReport.getId(), savedReport.getTargetType(), savedReport.getTargetId(), reportImages.size());

        return reportMapper.toResponse(savedReport, uploadsKeys);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponse getMyReport(Long userId, Long reportId) {
        Report report = reportRepository.findByIdAndReporterId(reportId, userId)
                .orElseThrow(() -> new AppException(AppError.REPORT_NOT_FOUND));

        List<ReportImage> images = reportImageRepository.findByReportId(reportId);
        List<String> imageUrls = images.stream()
                .map(ReportImage::getImageUrl)
                .toList();

        return reportMapper.toResponse(report, imageUrls);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> getMyReports(Long userId, Pageable pageable) {
        Page<Report> page = reportRepository.findByReporterId(userId, pageable);
        if (page.isEmpty()) {
            return PageResponse.of(page.map(r -> reportMapper.toResponse(r, List.of())));
        }

        List<Long> reportIds = page.getContent().stream()
                .map(Report::getId)
                .toList();
        List<ReportImage> allImages = reportImageRepository.findByReportIdIn(reportIds);
        Map<Long, List<String>> imageMap = allImages.stream()
                .collect(Collectors.groupingBy(
                        img -> img.getReport().getId(),
                        Collectors.mapping(ReportImage::getImageUrl, Collectors.toList())
                ));

        Page<ReportResponse> responsePage = page.map(r ->
                reportMapper.toResponse(r, imageMap.getOrDefault(r.getId(), List.of()))
        );

        return PageResponse.of(responsePage);
    }

    private String requireMediaKind(String verifiedKey, MediaKind... allowed) {
        MediaKind actual = FileKey.mediaKindOf(verifiedKey);
        if (actual == null || !List.of(allowed).contains(actual)) {
            throw new AppException(AppError.INVALID_FILE_TYPE);
        }
        return FileKey.toUploads(verifiedKey);
    }

    private void requireOwned(String key, Long userId) {
        if (!FileKey.isOwnedBy(key, userId)) {
            throw new AppException(AppError.FILE_ACCESS_DENIED);
        }
    }
}
