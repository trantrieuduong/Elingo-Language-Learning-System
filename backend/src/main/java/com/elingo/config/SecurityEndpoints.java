package com.elingo.config;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SecurityEndpoints {

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
}
