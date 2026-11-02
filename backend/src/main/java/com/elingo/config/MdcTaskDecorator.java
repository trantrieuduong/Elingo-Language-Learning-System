package com.elingo.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

public class MdcTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> callerContext = MDC.getCopyOfContextMap();   // chạy ở thread gọi

        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();    // chạy ở thread thực thi
            try {
                if (callerContext != null) MDC.setContextMap(callerContext);
                else MDC.clear();
                runnable.run();
            } finally {
                if (previous != null) MDC.setContextMap(previous);
                else MDC.clear();
            }
        };
    }
}
