package com.elingo.admin.audit.controller;

import com.elingo.admin.audit.dto.request.AdminAuditLogFilterRequest;
import com.elingo.admin.audit.dto.response.AdminAuditLogResponse;
import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import com.elingo.admin.audit.service.AdminAuditService;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AdminAuditControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminAuditService adminAuditService;

    @InjectMocks
    private AdminAuditController adminAuditController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminAuditController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Nested
    @DisplayName("search")
    class SearchTests {

        @Test
        @DisplayName("Search audit logs successfully with default parameters")
        void search_DefaultParams_Success() throws Exception {
            AdminAuditLogResponse logResponse = new AdminAuditLogResponse(
                    1L,
                    99L,
                    AdminAuditAction.USER_LOCK,
                    AdminAuditTargetType.USER,
                    "10",
                    "Policy violation",
                    "127.0.0.1",
                    LocalDateTime.now()
            );

            PageResponse<AdminAuditLogResponse> pageResponse = new PageResponse<>(
                    List.of(logResponse), 1, 20, 1, 1, true
            );

            when(adminAuditService.search(any(AdminAuditLogFilterRequest.class), any(Pageable.class)))
                    .thenReturn(pageResponse);

            mockMvc.perform(get("/admin/audit-logs")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].id").value(1L))
                    .andExpect(jsonPath("$.data.content[0].action").value("USER_LOCK"))
                    .andExpect(jsonPath("$.data.content[0].targetType").value("USER"));

            verify(adminAuditService).search(any(AdminAuditLogFilterRequest.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Search audit logs successfully with all filter query parameters")
        void search_WithFilterParameters_Success() throws Exception {
            PageResponse<AdminAuditLogResponse> emptyPage = new PageResponse<>(
                    List.of(), 1, 10, 0, 0, true
            );

            when(adminAuditService.search(any(AdminAuditLogFilterRequest.class), any(Pageable.class)))
                    .thenReturn(emptyPage);

            mockMvc.perform(get("/admin/audit-logs")
                            .param("adminId", "5")
                            .param("action", "POST_APPROVE")
                            .param("targetType", "POST")
                            .param("targetId", "100")
                            .param("from", "2026-11-01T00:00:00")
                            .param("to", "2026-11-10T23:59:59")
                            .param("page", "0")
                            .param("size", "10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isEmpty());

            verify(adminAuditService).search(any(AdminAuditLogFilterRequest.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Return BAD_REQUEST when from date is after to date")
        void search_WhenFromAfterTo_ShouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/admin/audit-logs")
                            .param("from", "2026-11-20T00:00:00")
                            .param("to", "2026-11-10T00:00:00")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Return BAD_REQUEST when adminId is non-positive")
        void search_WhenAdminIdNonPositive_ShouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/admin/audit-logs")
                            .param("adminId", "-1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Return BAD_REQUEST when targetId exceeds maximum length")
        void search_WhenTargetIdExceedsMaxLength_ShouldReturnBadRequest() throws Exception {
            String overlyLongTargetId = "X".repeat(65);

            mockMvc.perform(get("/admin/audit-logs")
                            .param("targetId", overlyLongTargetId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("getActions")
    class GetActionsTests {

        @Test
        @DisplayName("Get all audit actions successfully")
        void getActions_Success() throws Exception {
            mockMvc.perform(get("/admin/audit-logs/actions")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(AdminAuditAction.values().length)))
                    .andExpect(jsonPath("$.data", containsInAnyOrder(
                            Arrays.stream(AdminAuditAction.values())
                                    .map(Enum::name)
                                    .toArray(String[]::new)
                    )));
        }
    }

    @Nested
    @DisplayName("getTargetTypes")
    class GetTargetTypesTests {

        @Test
        @DisplayName("Get all audit target types successfully")
        void getTargetTypes_Success() throws Exception {
            mockMvc.perform(get("/admin/audit-logs/target-types")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(AdminAuditTargetType.values().length)))
                    .andExpect(jsonPath("$.data", containsInAnyOrder(
                            Arrays.stream(AdminAuditTargetType.values())
                                    .map(Enum::name)
                                    .toArray(String[]::new)
                    )));
        }
    }
}
