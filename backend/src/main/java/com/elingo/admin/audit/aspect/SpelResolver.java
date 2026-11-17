package com.elingo.admin.audit.aspect;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public final class SpelResolver {

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final Map<String, Expression> CACHE = new ConcurrentHashMap<>();

    private SpelResolver() {}

    /** @return chuỗi kết quả, hoặc null nếu biểu thức rỗng / ra null. */
    public static String resolve(String expression, JoinPoint joinPoint, Object result) {
        if (expression == null || expression.isBlank()) {
            return null;
        }

        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] names = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        Object[] args = joinPoint.getArgs();
        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                context.setVariable(names[i], args[i]);
            }
        }
        context.setVariable("result", result);

        Expression parsed = CACHE.computeIfAbsent(expression, PARSER::parseExpression);
        Object value = parsed.getValue(context);
        return value == null ? null : String.valueOf(value);
    }
}
