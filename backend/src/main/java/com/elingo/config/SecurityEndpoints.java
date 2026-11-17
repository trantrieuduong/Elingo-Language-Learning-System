package com.elingo.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class SecurityEndpoints {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final HttpMethod[] GET_ONLY = { HttpMethod.GET };
    private static final HttpMethod[] POST_ONLY = { HttpMethod.POST };
    private static final HttpMethod[] ALL_METHODS = HttpMethod.values();

    /**
     * Danh sách các pattern cần ưu tiên bắt buộc xác thực (authenticated) trước khi duyệt PUBLIC_ENDPOINTS.
     * Cần thiết vì các wildcard như /users/* hoặc /users/* /badges trong PUBLIC_ENDPOINTS sẽ khớp luôn cả segment "me".
     * Khi có thêm các endpoint cá nhân hoặc tài nguyên nhạy cảm có nguy cơ bị wildcard match nhầm, hãy bổ sung vào đây.
     */
    public static final List<String> AUTHENTICATED_ENDPOINTS = List.of(
            "/users/me",
            "/users/me/**"
    );

    /**
     * Danh sách các endpoint công khai kèm HttpMethod tương ứng.
     */
    public static final Map<String, HttpMethod[]> PUBLIC_ENDPOINTS = Map.ofEntries(
            Map.entry("/auth/login",                    POST_ONLY),
            Map.entry("/auth/signup",                   POST_ONLY),
            Map.entry("/auth/google",                   POST_ONLY),
            Map.entry("/auth/refresh",                  POST_ONLY),
            Map.entry("/auth/logout",                   POST_ONLY),
            Map.entry("/auth/password-reset/**",        POST_ONLY),
            Map.entry("/auth/account-verifications/**", POST_ONLY),
            Map.entry("/webhooks/payments/**",          POST_ONLY),
            Map.entry("/premium/plans",                 GET_ONLY),
            Map.entry("/swagger-ui/**",                 ALL_METHODS),
            Map.entry("/swagger-ui.html",               ALL_METHODS),
            Map.entry("/v3/api-docs/**",                ALL_METHODS),
            Map.entry("/v3/api-docs*/**",               ALL_METHODS),
            Map.entry("/ws/**",                         ALL_METHODS),
            Map.entry("/decks",                         GET_ONLY),
            Map.entry("/lessons",                       GET_ONLY),
            Map.entry("/tags",                          GET_ONLY),
            Map.entry("/cefr-levels",                   GET_ONLY),
            Map.entry("/post-groups",                   GET_ONLY),
            Map.entry("/posts",                         GET_ONLY),
            Map.entry("/users/*",                       GET_ONLY),
            Map.entry("/users/*/posts",                 GET_ONLY),
            Map.entry("/users/*/badges",                GET_ONLY),
            Map.entry("/users/*/levels",                GET_ONLY),
            Map.entry("/users/*/streaks",               GET_ONLY),
            Map.entry("/gamification/leaderboards",     GET_ONLY),
            Map.entry("/gamification/levels",           GET_ONLY),
            Map.entry("/gamification/leaderboards/history", GET_ONLY)
    );

    /**
     * Kiểm tra xem request có thuộc diện endpoint công khai (public) hay không.
     *
     * @param request HttpServletRequest nhận vào
     * @return true nếu là endpoint công khai và khớp HttpMethod, ngược lại false
     */
    public boolean isPublic(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();

        // Ưu tiên kiểm tra các endpoint bắt buộc xác thực trước
        for (String pattern : AUTHENTICATED_ENDPOINTS) {
            if (pathMatcher.match(pattern, path)) {
                return false;
            }
        }

        // So khớp với danh sách PUBLIC_ENDPOINTS
        for (Map.Entry<String, HttpMethod[]> entry : PUBLIC_ENDPOINTS.entrySet()) {
            String pattern = entry.getKey();
            HttpMethod[] methods = entry.getValue();

            if (pathMatcher.match(pattern, path)) {
                boolean methodMatches = Arrays.stream(methods)
                        .anyMatch(m -> m.name().equalsIgnoreCase(method));
                if (methodMatches) {
                    return true;
                }
            }
        }

        return false;
    }
}
