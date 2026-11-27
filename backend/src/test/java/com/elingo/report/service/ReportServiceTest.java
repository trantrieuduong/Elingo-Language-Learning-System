package com.elingo.report.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.common.event.FileAttachedEvent;
import com.elingo.common.event.NotificationEvent;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.report.dto.request.AdminReportSearchRequest;
import com.elingo.report.dto.request.CreateReportRequest;
import com.elingo.report.dto.response.AdminReportResponse;
import com.elingo.report.dto.response.ReportResponse;
import com.elingo.report.entity.Report;
import com.elingo.report.entity.ReportImage;
import com.elingo.report.entity.ReportStatus;
import com.elingo.report.entity.ReportTargetType;
import com.elingo.report.mapper.ReportMapper;
import com.elingo.report.repository.ReportImageRepository;
import com.elingo.report.repository.ReportRepository;
import com.elingo.report.service.impl.ReportServiceImpl;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReportImageRepository reportImageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReportMapper reportMapper;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private ReportChecker postChecker;

    @Mock
    private ReportChecker commentChecker;

    @Mock
    private ReportChecker userChecker;

    @Mock
    private ReportChecker deckChecker;

    @Mock
    private ReportChecker lessonChecker;

    private ReportService reportService;

    private static final Long USER_ID = 1L;
    private static final Long TARGET_ID = 100L;
    private static final Long REPORT_ID = 10L;

    @BeforeEach
    void setUp() {
        when(postChecker.supports()).thenReturn(ReportTargetType.POST);
        when(commentChecker.supports()).thenReturn(ReportTargetType.COMMENT);
        when(userChecker.supports()).thenReturn(ReportTargetType.USER);
        when(deckChecker.supports()).thenReturn(ReportTargetType.DECK);
        when(lessonChecker.supports()).thenReturn(ReportTargetType.LESSON);

        reportService = new ReportServiceImpl(
                reportRepository,
                reportImageRepository,
                userRepository,
                reportMapper,
                publisher,
                List.of(postChecker, commentChecker, userChecker, deckChecker, lessonChecker)
        );
    }

    private User createUser(Long id) {
        return User.builder().id(id).username("reporter").build();
    }

    private Report buildReport(Long id, Long reporterId, ReportTargetType targetType, Long targetId) {
        return Report.builder()
                .id(id)
                .reporter(createUser(reporterId))
                .targetType(targetType)
                .targetId(targetId)
                .description("Violation description")
                .status(ReportStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private ReportResponse buildReportResponse(Long id, List<String> imageUrls) {
        return new ReportResponse(
                id,
                ReportTargetType.POST,
                TARGET_ID,
                "Violation description",
                ReportStatus.PENDING,
                imageUrls,
                null,
                null,
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("create")
    class CreateReportTests {

        @Test
        @DisplayName("Create report successfully without images")
        void create_Success_WithoutImages() {
            User user = createUser(USER_ID);
            CreateReportRequest request = new CreateReportRequest(
                    ReportTargetType.POST,
                    TARGET_ID,
                    "Inappropriate post content",
                    null
            );

            Report savedReport = buildReport(REPORT_ID, USER_ID, ReportTargetType.POST, TARGET_ID);
            ReportResponse expectedResponse = buildReportResponse(REPORT_ID, List.of());

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                    USER_ID, ReportTargetType.POST, TARGET_ID, ReportStatus.PENDING
            )).thenReturn(false);
            when(reportRepository.save(any(Report.class))).thenReturn(savedReport);
            when(reportMapper.toResponse(eq(savedReport), eq(List.of()))).thenReturn(expectedResponse);

            ReportResponse actualResponse = reportService.create(USER_ID, request);

            assertThat(actualResponse).isEqualTo(expectedResponse);
            verify(postChecker).check(TARGET_ID, USER_ID);
            verify(reportRepository).save(any(Report.class));
            verify(reportImageRepository, never()).saveAll(anyList());
            verify(publisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Create report successfully with multiple valid images")
        void create_Success_WithImages() {
            User user = createUser(USER_ID);
            List<String> verifiedKeys = List.of(
                    "verified/1/evidence1.png",
                    "verified/1/evidence2.jpg"
            );
            CreateReportRequest request = new CreateReportRequest(
                    ReportTargetType.POST,
                    TARGET_ID,
                    "Inappropriate post content with evidence",
                    verifiedKeys
            );

            Report savedReport = buildReport(REPORT_ID, USER_ID, ReportTargetType.POST, TARGET_ID);
            List<String> expectedUploadsKeys = List.of(
                    "uploads/1/evidence1.png",
                    "uploads/1/evidence2.jpg"
            );
            ReportResponse expectedResponse = buildReportResponse(REPORT_ID, expectedUploadsKeys);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                    USER_ID, ReportTargetType.POST, TARGET_ID, ReportStatus.PENDING
            )).thenReturn(false);
            when(reportRepository.save(any(Report.class))).thenReturn(savedReport);
            when(reportMapper.toResponse(eq(savedReport), eq(expectedUploadsKeys))).thenReturn(expectedResponse);

            ReportResponse actualResponse = reportService.create(USER_ID, request);

            assertThat(actualResponse).isEqualTo(expectedResponse);
            verify(postChecker).check(TARGET_ID, USER_ID);

            ArgumentCaptor<List<ReportImage>> imagesCaptor = ArgumentCaptor.forClass(List.class);
            verify(reportImageRepository).saveAll(imagesCaptor.capture());
            List<ReportImage> savedImages = imagesCaptor.getValue();
            assertThat(savedImages).hasSize(2);
            assertThat(savedImages.get(0).getImageUrl()).isEqualTo("uploads/1/evidence1.png");
            assertThat(savedImages.get(1).getImageUrl()).isEqualTo("uploads/1/evidence2.jpg");

            verify(publisher, times(2)).publishEvent(any(FileAttachedEvent.class));
        }

        @Test
        @DisplayName("Create report successfully filters duplicate images in request")
        void create_Success_FiltersDuplicateImages() {
            User user = createUser(USER_ID);
            List<String> duplicateKeys = List.of(
                    "verified/1/evidence.png",
                    "verified/1/evidence.png"
            );
            CreateReportRequest request = new CreateReportRequest(
                    ReportTargetType.POST,
                    TARGET_ID,
                    "Post with duplicated image keys",
                    duplicateKeys
            );

            Report savedReport = buildReport(REPORT_ID, USER_ID, ReportTargetType.POST, TARGET_ID);
            List<String> expectedUploadsKeys = List.of("uploads/1/evidence.png");
            ReportResponse expectedResponse = buildReportResponse(REPORT_ID, expectedUploadsKeys);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                    USER_ID, ReportTargetType.POST, TARGET_ID, ReportStatus.PENDING
            )).thenReturn(false);
            when(reportRepository.save(any(Report.class))).thenReturn(savedReport);
            when(reportMapper.toResponse(eq(savedReport), eq(expectedUploadsKeys))).thenReturn(expectedResponse);

            ReportResponse actualResponse = reportService.create(USER_ID, request);

            assertThat(actualResponse).isEqualTo(expectedResponse);
            verify(publisher, times(1)).publishEvent(any(FileAttachedEvent.class));
        }

        @Test
        @DisplayName("Throw USER_NOT_FOUND when reporter does not exist")
        void create_ThrowUserNotFound_WhenReporterDoesNotExist() {
            CreateReportRequest request = new CreateReportRequest(
                    ReportTargetType.POST,
                    TARGET_ID,
                    "Inappropriate post content",
                    null
            );

            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reportService.create(USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));

            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throw AppException when checker throws (e.g. CANNOT_REPORT_SELF)")
        void create_ThrowException_WhenCheckerFails() {
            User user = createUser(USER_ID);
            CreateReportRequest request = new CreateReportRequest(
                    ReportTargetType.POST,
                    TARGET_ID,
                    "Trying to report own post",
                    null
            );

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            doThrow(new AppException(AppError.CANNOT_REPORT_SELF))
                    .when(postChecker).check(TARGET_ID, USER_ID);

            assertThatThrownBy(() -> reportService.create(USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CANNOT_REPORT_SELF));

            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throw REPORT_ALREADY_EXISTS when user has pending report for target")
        void create_ThrowReportAlreadyExists_WhenPendingReportExists() {
            User user = createUser(USER_ID);
            CreateReportRequest request = new CreateReportRequest(
                    ReportTargetType.POST,
                    TARGET_ID,
                    "Reporting again while pending",
                    null
            );

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                    USER_ID, ReportTargetType.POST, TARGET_ID, ReportStatus.PENDING
            )).thenReturn(true);

            assertThatThrownBy(() -> reportService.create(USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.REPORT_ALREADY_EXISTS));

            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throw FILE_ACCESS_DENIED when image is not owned by reporter")
        void create_ThrowFileAccessDenied_WhenImageNotOwnedByUser() {
            User user = createUser(USER_ID);
            List<String> unownedKeys = List.of("verified/999/other-user-image.png");
            CreateReportRequest request = new CreateReportRequest(
                    ReportTargetType.POST,
                    TARGET_ID,
                    "Reporting with someone else's image",
                    unownedKeys
            );

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                    USER_ID, ReportTargetType.POST, TARGET_ID, ReportStatus.PENDING
            )).thenReturn(false);

            assertThatThrownBy(() -> reportService.create(USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.FILE_ACCESS_DENIED));

            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throw INVALID_FILE_TYPE when file is not an allowed image")
        void create_ThrowInvalidFileType_WhenFileIsNotImage() {
            User user = createUser(USER_ID);
            List<String> invalidKeys = List.of("verified/1/malicious.pdf");
            CreateReportRequest request = new CreateReportRequest(
                    ReportTargetType.POST,
                    TARGET_ID,
                    "Reporting with a pdf document",
                    invalidKeys
            );

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                    USER_ID, ReportTargetType.POST, TARGET_ID, ReportStatus.PENDING
            )).thenReturn(false);

            assertThatThrownBy(() -> reportService.create(USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_FILE_TYPE));

            verify(reportRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getMyReport")
    class GetMyReportTests {

        @Test
        @DisplayName("Get my report successfully when report exists and belongs to user")
        void getMyReport_Success() {
            Report report = buildReport(REPORT_ID, USER_ID, ReportTargetType.POST, TARGET_ID);
            List<ReportImage> images = List.of(
                    ReportImage.builder().id(101L).report(report).imageUrl("uploads/1/img1.png").build(),
                    ReportImage.builder().id(102L).report(report).imageUrl("uploads/1/img2.png").build()
            );
            List<String> imageUrls = List.of("uploads/1/img1.png", "uploads/1/img2.png");
            ReportResponse expectedResponse = buildReportResponse(REPORT_ID, imageUrls);

            when(reportRepository.findByIdAndReporterId(REPORT_ID, USER_ID)).thenReturn(Optional.of(report));
            when(reportImageRepository.findByReportId(REPORT_ID)).thenReturn(images);
            when(reportMapper.toResponse(report, imageUrls)).thenReturn(expectedResponse);

            ReportResponse actualResponse = reportService.getMyReport(USER_ID, REPORT_ID);

            assertThat(actualResponse).isEqualTo(expectedResponse);
        }

        @Test
        @DisplayName("Throw REPORT_NOT_FOUND when report does not exist or belongs to another user")
        void getMyReport_ThrowReportNotFound_WhenNotFound() {
            when(reportRepository.findByIdAndReporterId(REPORT_ID, USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reportService.getMyReport(USER_ID, REPORT_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.REPORT_NOT_FOUND));

            verify(reportImageRepository, never()).findByReportId(any());
        }
    }

    @Nested
    @DisplayName("getMyReports")
    class GetMyReportsTests {

        @Test
        @DisplayName("Get my reports successfully when no reports found")
        void getMyReports_Success_WhenEmpty() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Report> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(reportRepository.findByReporterId(USER_ID, pageable)).thenReturn(emptyPage);

            PageResponse<ReportResponse> result = reportService.getMyReports(USER_ID, pageable);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
            verify(reportImageRepository, never()).findByReportIdIn(any());
        }

        @Test
        @DisplayName("Get my reports successfully with batch images mapping")
        void getMyReports_Success_WithBatchImages() {
            Pageable pageable = PageRequest.of(0, 10);
            Report report1 = buildReport(1L, USER_ID, ReportTargetType.POST, 100L);
            Report report2 = buildReport(2L, USER_ID, ReportTargetType.USER, 200L);
            Page<Report> page = new PageImpl<>(List.of(report1, report2), pageable, 2);

            ReportImage img1 = ReportImage.builder().id(11L).report(report1).imageUrl("uploads/1/p1.png").build();
            ReportImage img2 = ReportImage.builder().id(12L).report(report1).imageUrl("uploads/1/p2.png").build();
            ReportImage img3 = ReportImage.builder().id(13L).report(report2).imageUrl("uploads/1/p3.png").build();

            when(reportRepository.findByReporterId(USER_ID, pageable)).thenReturn(page);
            when(reportImageRepository.findByReportIdIn(List.of(1L, 2L))).thenReturn(List.of(img1, img2, img3));

            ReportResponse resp1 = buildReportResponse(1L, List.of("uploads/1/p1.png", "uploads/1/p2.png"));
            ReportResponse resp2 = buildReportResponse(2L, List.of("uploads/1/p3.png"));

            when(reportMapper.toResponse(report1, List.of("uploads/1/p1.png", "uploads/1/p2.png"))).thenReturn(resp1);
            when(reportMapper.toResponse(report2, List.of("uploads/1/p3.png"))).thenReturn(resp2);

            PageResponse<ReportResponse> result = reportService.getMyReports(USER_ID, pageable);

            assertThat(result.content()).hasSize(2);
            assertThat(result.totalElements()).isEqualTo(2);
            assertThat(result.content().get(0)).isEqualTo(resp1);
            assertThat(result.content().get(1)).isEqualTo(resp2);
        }
    }

    private AdminReportResponse buildAdminReportResponse(Long id, ReportStatus status, List<String> images) {
        return new AdminReportResponse(
                id, ReportTargetType.POST, TARGET_ID, "Violation description", status,
                USER_ID, "reporter", images, null, null, null, null,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("searchReportsForAdmin")
    class SearchReportsForAdmin {

        @Test
        @DisplayName("should return empty page without querying images when no reports match")
        void shouldReturnEmptyPage_whenNoReportsMatch() {
            // given
            AdminReportSearchRequest filter = new AdminReportSearchRequest(
                    ReportStatus.PENDING, null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 20);
            Page<Report> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(reportRepository.searchForAdmin(
                    ReportStatus.PENDING, null, null, null, null, null, pageable))
                    .thenReturn(emptyPage);

            // when
            PageResponse<AdminReportResponse> result = reportService.searchReportsForAdmin(filter, pageable);

            // then
            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
            verify(reportImageRepository, never()).findByReportIdIn(any());
        }

        @Test
        @DisplayName("should return paged reports with batch-loaded images and date bounds")
        void shouldReturnPagedReports_whenReportsExist() {
            // given
            LocalDate from = LocalDate.of(2026, 3, 1);
            LocalDate to = LocalDate.of(2026, 3, 15);
            AdminReportSearchRequest filter = new AdminReportSearchRequest(
                    ReportStatus.PENDING, ReportTargetType.POST, USER_ID, TARGET_ID, from, to);
            Pageable pageable = PageRequest.of(0, 20);

            Report report = buildReport(REPORT_ID, USER_ID, ReportTargetType.POST, TARGET_ID);
            Page<Report> page = new PageImpl<>(List.of(report), pageable, 1);
            ReportImage img = ReportImage.builder().id(1L).report(report).imageUrl("uploads/1/ev.png").build();
            AdminReportResponse expected = buildAdminReportResponse(REPORT_ID, ReportStatus.PENDING, List.of("uploads/1/ev.png"));

            when(reportRepository.searchForAdmin(
                    ReportStatus.PENDING,
                    ReportTargetType.POST,
                    USER_ID,
                    TARGET_ID,
                    from.atStartOfDay(),
                    to.plusDays(1).atStartOfDay(),
                    pageable
            )).thenReturn(page);
            when(reportImageRepository.findByReportIdIn(List.of(REPORT_ID))).thenReturn(List.of(img));
            when(reportMapper.toAdminResponse(report, List.of("uploads/1/ev.png"))).thenReturn(expected);

            // when
            PageResponse<AdminReportResponse> result = reportService.searchReportsForAdmin(filter, pageable);

            // then
            assertThat(result.content()).containsExactly(expected);
            assertThat(result.totalElements()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("getReportByIdForAdmin")
    class GetReportByIdForAdmin {

        @Test
        @DisplayName("should return admin report detail with images when report exists")
        void shouldReturnDetail_whenReportExists() {
            // given
            Report report = buildReport(REPORT_ID, USER_ID, ReportTargetType.POST, TARGET_ID);
            ReportImage img = ReportImage.builder().id(1L).report(report).imageUrl("uploads/1/ev.png").build();
            AdminReportResponse expected = buildAdminReportResponse(REPORT_ID, ReportStatus.PENDING, List.of("uploads/1/ev.png"));

            when(reportRepository.findByIdWithUsers(REPORT_ID)).thenReturn(Optional.of(report));
            when(reportImageRepository.findByReportId(REPORT_ID)).thenReturn(List.of(img));
            when(reportMapper.toAdminResponse(report, List.of("uploads/1/ev.png"))).thenReturn(expected);

            // when
            AdminReportResponse result = reportService.getReportByIdForAdmin(REPORT_ID);

            // then
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("should throw REPORT_NOT_FOUND when report does not exist")
        void shouldThrowNotFound_whenReportDoesNotExist() {
            // given
            when(reportRepository.findByIdWithUsers(REPORT_ID)).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> reportService.getReportByIdForAdmin(REPORT_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.REPORT_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("resolveReport")
    class ResolveReport {

        @Test
        @DisplayName("should resolve pending report, save, and publish notification event")
        void shouldResolveReport_whenPending() {
            // given
            Long adminId = 99L;
            User admin = User.builder().id(adminId).username("admin").build();
            Report report = buildReport(REPORT_ID, USER_ID, ReportTargetType.POST, TARGET_ID);
            AdminReportResponse expected = buildAdminReportResponse(REPORT_ID, ReportStatus.RESOLVED, List.of());

            when(reportRepository.findByIdWithUsers(REPORT_ID)).thenReturn(Optional.of(report));
            when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
            when(reportRepository.save(report)).thenReturn(report);
            when(reportImageRepository.findByReportId(REPORT_ID)).thenReturn(List.of());
            when(reportMapper.toAdminResponse(report, List.of())).thenReturn(expected);

            // when
            AdminReportResponse result = reportService.resolveReport(adminId, REPORT_ID, "Removed spam post");

            // then
            assertThat(result).isEqualTo(expected);
            assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
            assertThat(report.getResolvedBy()).isEqualTo(admin);
            assertThat(report.getResolvedAt()).isNotNull();
            assertThat(report.getResolutionNote()).isEqualTo("Removed spam post");

            ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
            verify(publisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().recipientId()).isEqualTo(USER_ID);
            assertThat(eventCaptor.getValue().refId()).isEqualTo(REPORT_ID);
        }

        @Test
        @DisplayName("should throw REPORT_NOT_FOUND when report does not exist")
        void shouldThrowNotFound_whenReportMissing() {
            // given
            when(reportRepository.findByIdWithUsers(REPORT_ID)).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> reportService.resolveReport(99L, REPORT_ID, "note"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.REPORT_NOT_FOUND));
            verify(reportRepository, never()).save(any());
            verify(publisher, never()).publishEvent(any());
        }

        @ParameterizedTest
        @EnumSource(value = ReportStatus.class, names = {"RESOLVED", "REJECTED"})
        @DisplayName("should throw REPORT_ALREADY_RESOLVED when report is not pending")
        void shouldThrowAlreadyResolved_whenNotPending(ReportStatus currentStatus) {
            // given
            Report report = buildReport(REPORT_ID, USER_ID, ReportTargetType.POST, TARGET_ID);
            report.setStatus(currentStatus);
            when(reportRepository.findByIdWithUsers(REPORT_ID)).thenReturn(Optional.of(report));

            // when / then
            assertThatThrownBy(() -> reportService.resolveReport(99L, REPORT_ID, "note"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.REPORT_ALREADY_RESOLVED));
            verify(reportRepository, never()).save(any());
            verify(publisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("should throw USER_NOT_FOUND when admin does not exist")
        void shouldThrowUserNotFound_whenAdminMissing() {
            // given
            Report report = buildReport(REPORT_ID, USER_ID, ReportTargetType.POST, TARGET_ID);
            when(reportRepository.findByIdWithUsers(REPORT_ID)).thenReturn(Optional.of(report));
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> reportService.resolveReport(99L, REPORT_ID, "note"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.USER_NOT_FOUND));
            verify(reportRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("rejectReport")
    class RejectReport {

        @Test
        @DisplayName("should reject pending report without publishing notification event")
        void shouldRejectReport_whenPending() {
            // given
            Long adminId = 99L;
            User admin = User.builder().id(adminId).username("admin").build();
            Report report = buildReport(REPORT_ID, USER_ID, ReportTargetType.POST, TARGET_ID);
            AdminReportResponse expected = buildAdminReportResponse(REPORT_ID, ReportStatus.REJECTED, List.of());

            when(reportRepository.findByIdWithUsers(REPORT_ID)).thenReturn(Optional.of(report));
            when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
            when(reportRepository.save(report)).thenReturn(report);
            when(reportImageRepository.findByReportId(REPORT_ID)).thenReturn(List.of());
            when(reportMapper.toAdminResponse(report, List.of())).thenReturn(expected);

            // when
            AdminReportResponse result = reportService.rejectReport(adminId, REPORT_ID, "No violation");

            // then
            assertThat(result).isEqualTo(expected);
            assertThat(report.getStatus()).isEqualTo(ReportStatus.REJECTED);
            assertThat(report.getResolvedBy()).isEqualTo(admin);
            assertThat(report.getResolvedAt()).isNotNull();
            assertThat(report.getResolutionNote()).isEqualTo("No violation");
            verify(publisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("should throw REPORT_ALREADY_RESOLVED when report is already processed")
        void shouldThrowAlreadyResolved_whenNotPending() {
            // given
            Report report = buildReport(REPORT_ID, USER_ID, ReportTargetType.POST, TARGET_ID);
            report.setStatus(ReportStatus.RESOLVED);
            when(reportRepository.findByIdWithUsers(REPORT_ID)).thenReturn(Optional.of(report));

            // when / then
            assertThatThrownBy(() -> reportService.rejectReport(99L, REPORT_ID, "No violation"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.REPORT_ALREADY_RESOLVED));
            verify(reportRepository, never()).save(any());
        }
    }
}
