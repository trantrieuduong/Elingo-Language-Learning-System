package com.elingo.config;

import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.ErrorDetail;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.service.RateLimiterService;
import com.elingo.common.enums.RateLimitRule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "IP-RATE-LIMIT-FILTER")
public class IpRateLimitFilter extends OncePerRequestFilter {

    private record Route(RequestMatcher matcher, RateLimitRule rule) {}

    private static final PathPatternRequestMatcher.Builder PATHS = PathPatternRequestMatcher.withDefaults();

    private static final List<Route> ROUTES = List.of(
            // BR-01
            new Route(PATHS.matcher(HttpMethod.POST, "/auth/signup"), RateLimitRule.SIGNUP_IP),
            // BR-02a
            new Route(PATHS.matcher(HttpMethod.POST, "/auth/login"), RateLimitRule.LOGIN_IP),
            // BR-03: gửi/gửi lại OTP, chung một bộ đếm
            new Route(PATHS.matcher(HttpMethod.POST, "/auth/password-reset/otp"), RateLimitRule.OTP_SEND_IP),
            new Route(PATHS.matcher(HttpMethod.POST, "/auth/account-verifications/otp"), RateLimitRule.OTP_SEND_IP),
            new Route(PATHS.matcher(HttpMethod.POST, "/users/me/email/otp"), RateLimitRule.OTP_SEND_IP),
            // BR-04: nhập OTP + đặt mật khẩu mới / email mới
            new Route(PATHS.matcher(HttpMethod.POST, "/auth/password-reset"), RateLimitRule.OTP_RESET_IP),
            new Route(PATHS.matcher(HttpMethod.POST, "/users/me/email"), RateLimitRule.OTP_RESET_IP),
            // BR-05: nhập OTP xác thực tài khoản
            new Route(PATHS.matcher(HttpMethod.POST, "/auth/account-verifications"), RateLimitRule.OTP_VERIFY_IP)
    );

    private final RateLimiterService limiter;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Route route = ROUTES.stream()
                .filter(r -> r.matcher().matches(request))
                .findFirst().orElse(null);

        if (route != null) {
            try {
                limiter.check(route.rule(), request.getRemoteAddr());
            } catch (AppException e) {
                AppError appError = AppError.RATE_LIMIT_EXCEEDED;

                log.warn("Rate limit exceeded rule={} ip={}", route.rule(), request.getRemoteAddr());

                response.setStatus(appError.getHttpStatusCode().value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                ApiResponse<?> apiResponse = ApiResponse.builder()
                        .success(false)
                        .message(appError.getMessage())
                        .requestId(MDC.get(RequestLoggingFilter.REQUEST_ID))
                        .errors(List.of(ErrorDetail.builder()
                                .code(appError.getCode())
                                .message(appError.getMessage())
                                .build()))
                        .build();

                response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                return;
            } catch (Exception e) {
                // Redis lỗi: fail-open, chỉ log
                log.warn("Rate limit check failed rule={} ip={}", route.rule(), request.getRemoteAddr(), e);
            }
        }

        filterChain.doFilter(request, response);
    }
}
