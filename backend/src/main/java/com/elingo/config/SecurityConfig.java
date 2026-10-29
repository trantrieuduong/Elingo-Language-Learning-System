package com.elingo.config;

import com.elingo.user.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final IpRateLimitFilter ipRateLimitFilter;
    private final List<String> ALLOW_ORIGINS;
    private static final HttpMethod[] GET_ONLY = { HttpMethod.GET };
    private static final HttpMethod[] POST_ONLY = { HttpMethod.POST };
    private static final HttpMethod[] ALL_METHODS = HttpMethod.values();
    private static final Map<String, HttpMethod[]> PUBLIC_ENDPOINTS = Map.ofEntries(
            Map.entry("/auth/login",        POST_ONLY),
            Map.entry("/auth/signup",       POST_ONLY),
            Map.entry("/auth/google",       POST_ONLY),
            Map.entry("/auth/refresh",      POST_ONLY),
            Map.entry("/auth/logout",       POST_ONLY),
            Map.entry("/auth/password-reset/**",        POST_ONLY),
            Map.entry("/auth/account-verifications/**", POST_ONLY),
            Map.entry("/swagger-ui/**",     ALL_METHODS),
            Map.entry("/swagger-ui.html",   ALL_METHODS),
            Map.entry("/v3/api-docs/**",    ALL_METHODS),
            Map.entry("/v3/api-docs*/**",   ALL_METHODS),
            Map.entry("/ws/**",             ALL_METHODS),
            Map.entry("/decks",             GET_ONLY),
            Map.entry("/lessons",           GET_ONLY),
            Map.entry("/tags",              GET_ONLY),
            Map.entry("/cefr-levels",       GET_ONLY),
            Map.entry("/post-groups",       GET_ONLY),
            Map.entry("/posts",             GET_ONLY),
            Map.entry("/users/*/posts",     GET_ONLY)
    );

    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            IpRateLimitFilter ipRateLimitFilter,
            @Value("#{'${app.front-end-domain}'.split(',')}") List<String> ALLOW_ORIGINS) {
        this.customUserDetailsService = customUserDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.ipRateLimitFilter = ipRateLimitFilter;
        this.ALLOW_ORIGINS = ALLOW_ORIGINS;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> {
                    PUBLIC_ENDPOINTS.forEach((path, methods) -> {
                        for (HttpMethod method : methods) {
                            auth.requestMatchers(method, path).permitAll();
                        }
                    });
                    auth.anyRequest().authenticated();
                })
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // REST API
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(ipRateLimitFilter, JwtAuthenticationFilter.class)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(
                customUserDetailsService.userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        corsConfiguration.setAllowedOrigins(ALLOW_ORIGINS);
        corsConfiguration.setAllowedMethods(List.of("*"));
        corsConfiguration.setAllowedHeaders(List.of("*"));
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setExposedHeaders(List.of("X-Request-Id"));

        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);

        return urlBasedCorsConfigurationSource;
    }
}
