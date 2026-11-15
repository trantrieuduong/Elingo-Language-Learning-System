package com.elingo.report.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.report.dto.request.CreateReportRequest;
import com.elingo.report.dto.response.ReportResponse;
import com.elingo.report.entity.ReportStatus;
import com.elingo.report.entity.ReportTargetType;
import com.elingo.report.service.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ReportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Long USER_ID = 1L;
    private static final Long REPORT_ID = 10L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reportController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new HandlerMethodArgumentResolver() {
                            @Override
                            public boolean supportsParameter(@NonNull MethodParameter parameter) {
                                return parameter.hasParameterAnnotation(CurrentUserId.class);
                            }

                            @Override
                            public Object resolveArgument(@NonNull MethodParameter parameter,
                                                          ModelAndViewContainer mavContainer,
                                                          @NonNull NativeWebRequest webRequest,
                                                          WebDataBinderFactory binderFactory) {
                                return USER_ID;
                            }
                        }
                )
                .build();
    }

    private ReportResponse buildReportResponse(Long id) {
        return new ReportResponse(
                id,
                ReportTargetType.POST,
                100L,
                "Violation content description",
                ReportStatus.PENDING,
                List.of(),
                null,
                null,
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("POST /reports")
    class CreateReportEndpointTests {

        @Test
        @DisplayName("Create report successfully returns 200 and ApiResponse with ReportResponse")
        void createReport_Success_Returns200() throws Exception {
            CreateReportRequest request = new CreateReportRequest(
                    ReportTargetType.POST,
                    100L,
                    "Inappropriate post content with spam",
                    List.of()
            );
            ReportResponse expectedResponse = buildReportResponse(REPORT_ID);

            when(reportService.create(eq(USER_ID), any(CreateReportRequest.class)))
                    .thenReturn(expectedResponse);

            mockMvc.perform(post("/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(REPORT_ID))
                    .andExpect(jsonPath("$.data.targetType").value("POST"))
                    .andExpect(jsonPath("$.data.targetId").value(100))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));

            verify(reportService).create(eq(USER_ID), any(CreateReportRequest.class));
        }

        @Test
        @DisplayName("Create report fails with 400 when description is shorter than 10 characters")
        void createReport_Fail_WhenDescriptionTooShort() throws Exception {
            CreateReportRequest request = new CreateReportRequest(
                    ReportTargetType.POST,
                    100L,
                    "Short",
                    null
            );

            mockMvc.perform(post("/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Create report fails with 400 when targetType is missing")
        void createReport_Fail_WhenTargetTypeIsNull() throws Exception {
            String jsonWithoutTargetType = """
                    {
                        "targetId": 100,
                        "description": "Inappropriate post content with spam"
                    }
                    """;

            mockMvc.perform(post("/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonWithoutTargetType))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Create report fails with 400 when targetId is not positive")
        void createReport_Fail_WhenTargetIdIsNotPositive() throws Exception {
            CreateReportRequest request = new CreateReportRequest(
                    ReportTargetType.POST,
                    -1L,
                    "Inappropriate post content with spam",
                    null
            );

            mockMvc.perform(post("/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Create report fails with 400 when business exception occurs (e.g. CANNOT_REPORT_SELF)")
        void createReport_Fail_WhenBusinessExceptionOccurs() throws Exception {
            CreateReportRequest request = new CreateReportRequest(
                    ReportTargetType.POST,
                    100L,
                    "Inappropriate post content with spam",
                    null
            );

            when(reportService.create(eq(USER_ID), any(CreateReportRequest.class)))
                    .thenThrow(new AppException(AppError.CANNOT_REPORT_SELF));

            mockMvc.perform(post("/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("CANNOT_REPORT_SELF"));
        }
    }

    @Nested
    @DisplayName("GET /reports")
    class GetMyReportsEndpointTests {

        @Test
        @DisplayName("Get my reports returns 200 with paginated PageResponse")
        void getMyReports_Success_Returns200() throws Exception {
            ReportResponse item1 = buildReportResponse(1L);
            ReportResponse item2 = buildReportResponse(2L);
            PageResponse<ReportResponse> pageResponse = new PageResponse<>(
                    List.of(item1, item2), 1, 20, 2, 1, true
            );

            when(reportService.getMyReports(eq(USER_ID), any(Pageable.class)))
                    .thenReturn(pageResponse);

            mockMvc.perform(get("/reports")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(2))
                    .andExpect(jsonPath("$.data.totalElements").value(2))
                    .andExpect(jsonPath("$.data.totalPages").value(1));

            verify(reportService).getMyReports(eq(USER_ID), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("GET /reports/{reportId}")
    class GetMyReportDetailEndpointTests {

        @Test
        @DisplayName("Get my report by id returns 200 with report details")
        void getMyReport_Success_Returns200() throws Exception {
            ReportResponse reportResponse = buildReportResponse(REPORT_ID);

            when(reportService.getMyReport(USER_ID, REPORT_ID)).thenReturn(reportResponse);

            mockMvc.perform(get("/reports/{reportId}", REPORT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(REPORT_ID))
                    .andExpect(jsonPath("$.data.targetType").value("POST"))
                    .andExpect(jsonPath("$.data.targetId").value(100));

            verify(reportService).getMyReport(USER_ID, REPORT_ID);
        }

        @Test
        @DisplayName("Get my report returns 404 when report not found or not owned by user")
        void getMyReport_Fail_WhenReportNotFound() throws Exception {
            when(reportService.getMyReport(USER_ID, REPORT_ID))
                    .thenThrow(new AppException(AppError.REPORT_NOT_FOUND));

            mockMvc.perform(get("/reports/{reportId}", REPORT_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("REPORT_NOT_FOUND"));
        }

        @Test
        @DisplayName("Get my report returns 400 when reportId is zero or negative")
        void getMyReport_Fail_WhenReportIdIsNotPositive() throws Exception {
            mockMvc.perform(get("/reports/{reportId}", 0))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
