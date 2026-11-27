package com.elingo.admin.community.report.service;

import com.elingo.admin.community.report.dto.request.RejectReportRequest;
import com.elingo.admin.community.report.dto.request.ResolveReportRequest;
import com.elingo.admin.community.report.service.impl.AdminCommunityReportServiceImpl;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.report.dto.request.AdminReportSearchRequest;
import com.elingo.report.dto.response.AdminReportResponse;
import com.elingo.report.entity.ReportStatus;
import com.elingo.report.entity.ReportTargetType;
import com.elingo.report.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("AdminCommunityReportService")
class AdminCommunityReportServiceTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private AdminCommunityReportServiceImpl adminCommunityReportService;

    private static final Long ADMIN_ID = 1L;
    private static final Long REPORT_ID = 42L;

    private static final AdminReportResponse SAMPLE_RESPONSE = new AdminReportResponse(
            REPORT_ID,
            ReportTargetType.POST,
            105L,
            "Spam links",
            ReportStatus.PENDING,
            12L,
            "jane_doe",
            List.of("https://cdn.elingo.com/r1.png"),
            null,
            null,
            null,
            null,
            LocalDateTime.now(),
            LocalDateTime.now()
    );

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("should delegate to reportService when sort field is allowed")
        void shouldDelegateToReportService_whenSortIsAllowed() {
            // given
            AdminReportSearchRequest filter = new AdminReportSearchRequest(
                    ReportStatus.PENDING, ReportTargetType.POST, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
            PageResponse<AdminReportResponse> expected = new PageResponse<>(
                    List.of(SAMPLE_RESPONSE), 1, 20, 1L, 1, true);

            when(reportService.searchReportsForAdmin(filter, pageable)).thenReturn(expected);

            // when
            PageResponse<AdminReportResponse> result = adminCommunityReportService.search(filter, pageable);

            // then
            assertThat(result).isEqualTo(expected);
            verify(reportService).searchReportsForAdmin(filter, pageable);
        }

        @ParameterizedTest
        @ValueSource(strings = {"status", "targetType", "description", "unknown"})
        @DisplayName("should throw INVALID_SORT when sort field is not in whitelist")
        void shouldThrowInvalidSort_whenSortFieldNotAllowed(String sortField) {
            // given
            AdminReportSearchRequest filter = new AdminReportSearchRequest(null, null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 20, Sort.by(sortField));

            // when / then
            assertThatThrownBy(() -> adminCommunityReportService.search(filter, pageable))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.INVALID_SORT));
            verify(reportService, never()).searchReportsForAdmin(any(), any());
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("should return report detail from reportService")
        void shouldReturnDetail_whenReportExists() {
            // given
            when(reportService.getReportByIdForAdmin(REPORT_ID)).thenReturn(SAMPLE_RESPONSE);

            // when
            AdminReportResponse result = adminCommunityReportService.getById(REPORT_ID);

            // then
            assertThat(result).isEqualTo(SAMPLE_RESPONSE);
            verify(reportService).getReportByIdForAdmin(REPORT_ID);
        }
    }

    @Nested
    @DisplayName("resolve")
    class Resolve {

        @Test
        @DisplayName("should pass resolutionNote when request is provided")
        void shouldPassNote_whenRequestProvided() {
            // given
            ResolveReportRequest request = new ResolveReportRequest("Confirmed violation");
            when(reportService.resolveReport(ADMIN_ID, REPORT_ID, "Confirmed violation"))
                    .thenReturn(SAMPLE_RESPONSE);

            // when
            AdminReportResponse result = adminCommunityReportService.resolve(ADMIN_ID, REPORT_ID, request);

            // then
            assertThat(result).isEqualTo(SAMPLE_RESPONSE);
            verify(reportService).resolveReport(ADMIN_ID, REPORT_ID, "Confirmed violation");
        }

        @Test
        @DisplayName("should pass null note when request is null")
        void shouldPassNullNote_whenRequestIsNull() {
            // given
            when(reportService.resolveReport(ADMIN_ID, REPORT_ID, null))
                    .thenReturn(SAMPLE_RESPONSE);

            // when
            AdminReportResponse result = adminCommunityReportService.resolve(ADMIN_ID, REPORT_ID, null);

            // then
            assertThat(result).isEqualTo(SAMPLE_RESPONSE);
            verify(reportService).resolveReport(ADMIN_ID, REPORT_ID, null);
        }
    }

    @Nested
    @DisplayName("reject")
    class Reject {

        @Test
        @DisplayName("should delegate rejection with reason to reportService")
        void shouldDelegateRejection_whenValidRequest() {
            // given
            RejectReportRequest request = new RejectReportRequest("No violation found");
            when(reportService.rejectReport(ADMIN_ID, REPORT_ID, "No violation found"))
                    .thenReturn(SAMPLE_RESPONSE);

            // when
            AdminReportResponse result = adminCommunityReportService.reject(ADMIN_ID, REPORT_ID, request);

            // then
            assertThat(result).isEqualTo(SAMPLE_RESPONSE);
            verify(reportService).rejectReport(ADMIN_ID, REPORT_ID, "No violation found");
        }
    }
}
