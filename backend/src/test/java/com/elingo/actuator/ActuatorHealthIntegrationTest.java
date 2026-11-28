package com.elingo.actuator;

import com.elingo.BaseIntegrationTest;
import com.elingo.auth.service.JwtService;
import com.elingo.user.entity.Role;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ActuatorHealthIntegrationTest extends BaseIntegrationTest {

    /**
     * /actuator/health chỉ có thể trả 200 (UP) hoặc 503 (DOWN).
     * Trên CI không có Redis thật nên có thể là 503, vì vậy chấp nhận cả hai.
     */
    private static final Matcher<Integer> UP_OR_DOWN =
            org.hamcrest.Matchers.anyOf(org.hamcrest.Matchers.is(200), org.hamcrest.Matchers.is(503));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private String adminToken;

    @BeforeEach
    void setUp() {
        adminUser = userRepository.save(User.builder()
                .username("admin_actuator_user")
                .email("admin_actuator@elingo.test")
                .passwordHash(passwordEncoder.encode("Password123@"))
                .fullName("Admin Actuator")
                .isActive(true)
                .isVerified(true)
                .role(Role.ADMIN)
                .build());
        adminToken = jwtService.generateAccessToken(adminUser.getId().toString(), adminUser.getRole().name());
    }

    @Nested
    @DisplayName("/actuator/health")
    class HealthEndpointTests {

        @Test
        @DisplayName("GET without token: 200 or 503, only status, no components")
        void testGetHealth_WithoutToken_OnlyStatus() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().is(UP_OR_DOWN))
                    .andExpect(jsonPath("$.status").exists())
                    .andExpect(jsonPath("$.components").doesNotExist());
        }

        @Test
        @DisplayName("HEAD without token: 200 or 503")
        void testHeadHealth_WithoutToken_Success() throws Exception {
            mockMvc.perform(head("/actuator/health"))
                    .andExpect(status().is(UP_OR_DOWN));
        }

        @Test
        @DisplayName("GET with admin token: 200 or 503, includes components")
        void testGetHealth_WithAdminToken_IncludesComponents() throws Exception {
            mockMvc.perform(get("/actuator/health")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().is(UP_OR_DOWN))
                    .andExpect(jsonPath("$.status").exists())
                    .andExpect(jsonPath("$.components").exists());
        }
    }

    @Nested
    @DisplayName("Other actuator endpoints (not exposed)")
    class OtherActuatorEndpointsTests {

        @Test
        @DisplayName("GET /actuator/env without token: Fail with 403")
        void testGetEnv_WithoutToken_Forbidden() throws Exception {
            mockMvc.perform(get("/actuator/env"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /actuator/env with admin token: Fail with 404")
        void testGetEnv_WithAdminToken_NotFound() throws Exception {
            mockMvc.perform(get("/actuator/env")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /actuator/metrics without token: Fail with 403")
        void testGetMetrics_WithoutToken_Forbidden() throws Exception {
            mockMvc.perform(get("/actuator/metrics"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /actuator/metrics with admin token: Fail with 404")
        void testGetMetrics_WithAdminToken_NotFound() throws Exception {
            mockMvc.perform(get("/actuator/metrics")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                    .andExpect(status().isNotFound());
        }
    }
}
