package com.elingo.admin.community.report.controller;

import com.elingo.admin.community.report.dto.request.RejectReportRequest;
import com.elingo.admin.community.report.dto.request.ResolveReportRequest;
import com.elingo.admin.community.report.service.AdminCommunityReportService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.report.dto.request.AdminReportSearchRequest;
import com.elingo.report.dto.response.AdminReportResponse;
import com.elingo.report.entity.ReportStatus;
import com.elingo.report.entity.ReportTargetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("AdminCommunityReportController")
class AdminCommunityReportControllerTest {

    @Mock
    private AdminCommunityReportService adminCommunityReportService;

    @InjectMocks
    private AdminCommunityReportController adminCommunityReportController;

    private static final Long ADMIN_ID = 1L;
    private static final Long REPORT_ID = 42L;

    private static final AdminReportResponse SAMPLE_RESPONSE = new AdminReportResponse(
            REPORT_ID, ReportTargetType.POST, 105L, "Spam links", ReportStatus.PENDING,
            12L, "jane_doe", List.of(), null, null, null, null,
            LocalDateTime.now(), LocalDateTime.now()
    );

    @Nested
    @DisplayName("searchReports")
    class SearchReports {

        @Test
        @DisplayName("should return ApiResponse with PageResponse")
        void shouldReturnPageResponse_whenValidRequest() {
            // given
            AdminReportSearchRequest filter = new AdminReportSearchRequest(
                    ReportStatus.PENDING, null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 20);
            PageResponse<AdminReportResponse> page = new PageResponse<>(
                    List.of(SAMPLE_RESPONSE), 1, 20, 1L, 1, true);

            when(adminCommunityReportService.search(filter, pageable)).thenReturn(page);

            // when
            ApiResponse<PageResponse<AdminReportResponse>> response =
                    adminCommunityReportController.searchReports(filter, pageable);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(page);
            verify(adminCommunityReportService).search(filter, pageable);
        }
    }

    @Nested
    @DisplayName("getReportById")
    class GetReportById {

        @Test
        @DisplayName("should return ApiResponse with report detail")
        void shouldReturnDetail_whenReportExists() {
            // given
            when(adminCommunityReportService.getById(REPORT_ID)).thenReturn(SAMPLE_RESPONSE);

            // when
            ApiResponse<AdminReportResponse> response = adminCommunityReportController.getReportById(REPORT_ID);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(SAMPLE_RESPONSE);
            verify(adminCommunityReportService).getById(REPORT_ID);
        }

        @Test
        @DisplayName("should propagate AppException when report is not found")
        void shouldPropagateException_whenNotFound() {
            // given
            when(adminCommunityReportService.getById(REPORT_ID))
                    .thenThrow(new AppException(AppError.REPORT_NOT_FOUND));

            // when / then
            assertThatThrownBy(() -> adminCommunityReportController.getReportById(REPORT_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.REPORT_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("resolveReport")
    class ResolveReport {

        @Test
        @DisplayName("should resolve report and return ApiResponse")
        void shouldResolveReport_whenValid() {
            // given
            ResolveReportRequest request = new ResolveReportRequest("Action taken");
            when(adminCommunityReportService.resolve(ADMIN_ID, REPORT_ID, request)).thenReturn(SAMPLE_RESPONSE);

            // when
            ApiResponse<AdminReportResponse> response =
                    adminCommunityReportController.resolveReport(ADMIN_ID, REPORT_ID, request);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(SAMPLE_RESPONSE);
            verify(adminCommunityReportService).resolve(ADMIN_ID, REPORT_ID, request);
        }
    }

    @Nested
    @DisplayName("rejectReport")
    class RejectReport {

        @Test
        @DisplayName("should reject report and return ApiResponse")
        void shouldRejectReport_whenValid() {
            // given
            RejectReportRequest request = new RejectReportRequest("No violation");
            when(adminCommunityReportService.reject(ADMIN_ID, REPORT_ID, request)).thenReturn(SAMPLE_RESPONSE);

            // when
            ApiResponse<AdminReportResponse> response =
                    adminCommunityReportController.rejectReport(ADMIN_ID, REPORT_ID, request);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(SAMPLE_RESPONSE);
            verify(adminCommunityReportService).reject(ADMIN_ID, REPORT_ID, request);
        }
    }
}
