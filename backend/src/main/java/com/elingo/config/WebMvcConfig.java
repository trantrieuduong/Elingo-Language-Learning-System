package com.elingo.config;

import com.elingo.common.annotation.CurrentUserIdValidator;
import com.elingo.common.annotation.IsAdminValidator;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@AllArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private CurrentUserIdValidator currentUserIdValidator;
    private IsAdminValidator isAdminValidator;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserIdValidator);
        resolvers.add(isAdminValidator);
    }
}
