package com.elingo.admin.audit.aspect;

import com.elingo.admin.audit.annotation.Audited;
import com.elingo.admin.audit.service.AdminAuditService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Ghi audit log CÙNG TRANSACTION với thao tác chính (atomic):
 * thao tác lỗi hoặc ghi log lỗi thì cả hai cùng rollback.
 * <p>
 * Cách làm: aspect bao quanh method (@Around) và tự mở một transaction
 * (PROPAGATION_REQUIRED). Trong transaction đó chạy method thật rồi ghi log.
 * - Nếu method có @Transactional: nó tham gia transaction của aspect.
 * - Nếu aspect nằm bên trong transaction của service: nó tham gia transaction đó.
 * Cả hai trường hợp đều chung một transaction.
 * <p>
 * Lưu ý: không đặt propagation = REQUIRES_NEW trên method có @Audited,
 * vì khi đó thao tác chính sẽ tách khỏi transaction chứa audit log.
 */
@Slf4j(topic = "ADMIN-AUDIT-ASPECT")
@Aspect
@Component
public class AdminAuditAspect {

    private final AdminAuditService auditService;
    private final CurrentAdminProvider currentAdmin;
    private final TransactionTemplate transactionTemplate;

    public AdminAuditAspect(AdminAuditService auditService,
                            CurrentAdminProvider currentAdmin,
                            PlatformTransactionManager transactionManager) {
        this.auditService = auditService;
        this.currentAdmin = currentAdmin;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Around("@annotation(audited)")
    public Object around(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        try {
            return transactionTemplate.execute(status -> {
                Object result;
                try {
                    result = joinPoint.proceed();
                } catch (RuntimeException | Error e) {
                    throw e; // TransactionTemplate rollback rồi ném lại nguyên lỗi
                } catch (Throwable t) {
                    throw new CheckedExceptionWrapper(t); // exception checked: bọc lại để rollback
                }

                // Ghi log lỗi thì để lỗi lan ra: cả thao tác chính cũng rollback.
                auditService.record(
                        currentAdmin.id(),
                        audited.action(),
                        audited.target(),
                        evaluate(audited.targetId(), joinPoint, result),
                        evaluate(audited.detail(), joinPoint, result),
                        currentAdmin.ip());
                return result;
            });
        } catch (CheckedExceptionWrapper w) {
            throw w.getCause(); // trả lại đúng exception gốc cho caller
        }
    }

    private String evaluate(String expression, ProceedingJoinPoint joinPoint, Object result) {
        try {
            return SpelResolver.resolve(expression, joinPoint, result);
        } catch (Exception e) {
            log.warn("Cannot evaluate audit expression '{}' on {}: {}",
                    expression, joinPoint.getSignature().toShortString(), e.getMessage());
            return null;
        }
    }

    private static final class CheckedExceptionWrapper extends RuntimeException {
        CheckedExceptionWrapper(Throwable cause) {
            super(cause);
        }
    }
}
