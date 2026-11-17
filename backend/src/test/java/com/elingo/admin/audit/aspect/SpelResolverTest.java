package com.elingo.admin.audit.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.expression.ParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SpelResolverTest {

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    record SampleResponse(Long id, String code) {}

    @Nested
    @DisplayName("resolve")
    class ResolveTests {

        @Test
        @DisplayName("Return null when expression is null, empty, or blank")
        void resolve_WhenExpressionNullOrBlank_ShouldReturnNull() {
            assertThat(SpelResolver.resolve(null, joinPoint, null)).isNull();
            assertThat(SpelResolver.resolve("", joinPoint, null)).isNull();
            assertThat(SpelResolver.resolve("   ", joinPoint, null)).isNull();
        }

        @Test
        @DisplayName("Evaluate SpEL expression based on method parameter variables")
        void resolve_WithMethodParameters_ShouldEvaluateCorrectly() {
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getParameterNames()).thenReturn(new String[]{"targetId", "status"});
            when(joinPoint.getArgs()).thenReturn(new Object[]{500L, "ACTIVE"});

            String targetIdResult = SpelResolver.resolve("#targetId", joinPoint, null);
            String combinedResult = SpelResolver.resolve("'ID: ' + #targetId + ' - ' + #status", joinPoint, null);

            assertThat(targetIdResult).isEqualTo("500");
            assertThat(combinedResult).isEqualTo("ID: 500 - ACTIVE");
        }

        @Test
        @DisplayName("Evaluate SpEL expression accessing result object properties")
        void resolve_WithResultVariable_ShouldEvaluateCorrectly() {
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getParameterNames()).thenReturn(new String[]{});
            when(joinPoint.getArgs()).thenReturn(new Object[]{});

            SampleResponse response = new SampleResponse(88L, "SUCCESS_CODE");

            String resultId = SpelResolver.resolve("#result.id()", joinPoint, response);
            String resultCode = SpelResolver.resolve("#result.code()", joinPoint, response);

            assertThat(resultId).isEqualTo("88");
            assertThat(resultCode).isEqualTo("SUCCESS_CODE");
        }

        @Test
        @DisplayName("Resolve successfully when signature parameter names array is null")
        void resolve_WhenParameterNamesNull_ShouldStillResolveResult() {
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getParameterNames()).thenReturn(null);
            when(joinPoint.getArgs()).thenReturn(new Object[]{});

            String evaluated = SpelResolver.resolve("#result", joinPoint, "ReturnedValue");

            assertThat(evaluated).isEqualTo("ReturnedValue");
        }

        @Test
        @DisplayName("Return null when variable does not exist or evaluates to null")
        void resolve_WhenValueIsNull_ShouldReturnNull() {
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getParameterNames()).thenReturn(new String[]{"emptyParam"});
            when(joinPoint.getArgs()).thenReturn(new Object[]{null});

            String evaluated = SpelResolver.resolve("#emptyParam", joinPoint, null);

            assertThat(evaluated).isNull();
        }

        @Test
        @DisplayName("Throw ParseException when SpEL expression syntax is invalid")
        void resolve_WhenExpressionSyntaxInvalid_ShouldThrowParseException() {
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getParameterNames()).thenReturn(new String[]{});
            when(joinPoint.getArgs()).thenReturn(new Object[]{});

            assertThatThrownBy(() -> SpelResolver.resolve("#[unclosed_bracket", joinPoint, null))
                    .isInstanceOf(ParseException.class);
        }
    }
}
