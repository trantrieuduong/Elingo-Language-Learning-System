package com.elingo.admin.audit.aspect;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CurrentAdminProviderTest {

    private CurrentAdminProvider currentAdminProvider;

    @Mock
    private HttpServletRequest httpServletRequest;

    @BeforeEach
    void setUp() {
        currentAdminProvider = new CurrentAdminProvider();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Nested
    @DisplayName("id")
    class IdTests {

        @Test
        @DisplayName("Return admin ID when SecurityContext has valid authentication with Long principal")
        void id_Success() {
            Long expectedAdminId = 123L;
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    expectedAdminId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);

            Long actualId = currentAdminProvider.id();

            assertThat(actualId).isEqualTo(expectedAdminId);
        }

        @Test
        @DisplayName("Throw IllegalStateException when SecurityContext has null authentication")
        void id_WhenAuthenticationIsNull_ShouldThrowIllegalStateException() {
            SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> currentAdminProvider.id())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("No authenticated admin in SecurityContext");
        }

        @Test
        @DisplayName("Throw IllegalStateException when authentication is not authenticated")
        void id_WhenNotAuthenticated_ShouldThrowIllegalStateException() {
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(false);

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);

            assertThatThrownBy(() -> currentAdminProvider.id())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("No authenticated admin in SecurityContext");
        }

        @Test
        @DisplayName("Throw IllegalStateException when principal is not of type Long")
        void id_WhenPrincipalNotLong_ShouldThrowIllegalStateException() {
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    "anonymousAdminString",
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);

            assertThatThrownBy(() -> currentAdminProvider.id())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("No authenticated admin in SecurityContext");
        }
    }

    @Nested
    @DisplayName("ip")
    class IpTests {

        @Test
        @DisplayName("Return client remote address when executing within servlet request context")
        void ip_WhenInServletRequest_ShouldReturnRemoteAddr() {
            when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.50");
            ServletRequestAttributes attributes = new ServletRequestAttributes(httpServletRequest);
            RequestContextHolder.setRequestAttributes(attributes);

            String ip = currentAdminProvider.ip();

            assertThat(ip).isEqualTo("192.168.1.50");
        }

        @Test
        @DisplayName("Return null when executing outside servlet request context")
        void ip_WhenOutsideServletRequest_ShouldReturnNull() {
            RequestContextHolder.resetRequestAttributes();

            String ip = currentAdminProvider.ip();

            assertThat(ip).isNull();
        }
    }
}
