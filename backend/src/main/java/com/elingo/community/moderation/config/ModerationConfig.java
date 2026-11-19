package com.elingo.community.moderation.config;

import com.elingo.common.enums.SystemConfigKey;
import com.elingo.common.service.SystemConfigService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class ModerationConfig {

    private static final Duration MODERATION_SERVICE_CONNECT = Duration.ofSeconds(2);
    private static final Duration MODERATION_SERVICE_READ = Duration.ofSeconds(10);
    private static final Duration OPENAI_CONNECT = Duration.ofSeconds(3);
    private static final Duration OPENAI_READ = Duration.ofSeconds(20);

    @Bean(name = "localModerationServiceClient")
    RestClient localModerationServiceClient(RestClient.Builder builder,
                                            ModerationProperties props) {
        ModerationProperties.ModerationService cfg = props.moderationService();
        RestClient.Builder b = builder.clone()
                .requestFactory(requestFactory(MODERATION_SERVICE_CONNECT, MODERATION_SERVICE_READ))
                .baseUrl(cfg.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        if (!cfg.token().isBlank()) {
            b.defaultHeader("X-Internal-Token", cfg.token());
        }
        return b.build();
    }

    @Bean(name = "openAiRestClient")
    RestClient openAiRestClient(RestClient.Builder builder,
                                SystemConfigService systemConfigService) {
        return builder.clone()
                .requestFactory(requestFactory(OPENAI_CONNECT, OPENAI_READ))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .requestInterceptor((request, body, execution) -> {
                    String apiKey = systemConfigService.getString(SystemConfigKey.MODERATION_OPENAI_API_KEY);
                    if (apiKey != null && !apiKey.isBlank()) {
                        request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    private static ClientHttpRequestFactory requestFactory(Duration connect, Duration read) {
        JdkClientHttpRequestFactory f = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(connect).build());
        f.setReadTimeout(read);
        return f;
    }
}
