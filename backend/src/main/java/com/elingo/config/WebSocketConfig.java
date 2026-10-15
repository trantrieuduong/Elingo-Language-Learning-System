package com.elingo.config;

import com.elingo.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final String[] ALLOW_ORIGINS;
    private final JwtService jwtService;

    public WebSocketConfig(@Value("#{'${app.front-end-domain}'.split(',')}")String[] allowOrigins,
                           JwtService jwtService) {
        this.ALLOW_ORIGINS = allowOrigins;
        this.jwtService = jwtService;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public @Nullable Message<?> preSend(Message<?> message, MessageChannel channel) {
                var acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (acc != null && StompCommand.CONNECT.equals(acc.getCommand())) {
                    String auth = acc.getFirstNativeHeader("Authorization");
                    if (auth == null || !auth.startsWith("Bearer ")) {
                        throw new MessagingException("Unauthorized");
                    }
                    String token = auth.substring(7);
                    try {
                        Claims claims = jwtService.getClaimsJws(token).getBody();
                        Long userId = Long.parseLong(claims.getSubject());
                        String role = claims.get("roles", String.class);
                        acc.setUser(UsernamePasswordAuthenticationToken.authenticated(
                                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        ));
                    } catch (JwtException | IllegalArgumentException ex) {
                        throw new MessagingException("Unauthorized");
                    }
                }
                return message;
            }
        });
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // client -> server: Gọi vào @MessageMapping
        registry.setApplicationDestinationPrefixes("/app");

        // /queue: server -> 1 client
        registry.enableSimpleBroker("/queue");

        // server -> 1 user cụ thể
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(ALLOW_ORIGINS);
    }
    
}
