package com.elingo.admin.audit.aspect;

import com.elingo.admin.audit.annotation.Audited;
import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import com.elingo.admin.audit.service.AdminAuditService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AdminAuditAspectTest {

    @Mock
    private AdminAuditService auditService;

    @Mock
    private CurrentAdminProvider currentAdmin;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionStatus transactionStatus;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private Audited audited;

    private AdminAuditAspect aspect;

    private static final Long ADMIN_ID = 42L;
    private static final String IP_ADDR = "10.0.0.1";

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        aspect = new AdminAuditAspect(auditService, currentAdmin, transactionManager);
    }

    @Nested
    @DisplayName("around")
    class AroundTests {

        @Test
        @DisplayName("Record audit log and commit transaction when target method completes normally")
        void around_Success() throws Throwable {
            when(joinPoint.proceed()).thenReturn("TargetMethodResult");
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getParameterNames()).thenReturn(new String[]{"targetUserId", "reason"});
            when(joinPoint.getArgs()).thenReturn(new Object[]{100L, "Spam content"});

            when(currentAdmin.id()).thenReturn(ADMIN_ID);
            when(currentAdmin.ip()).thenReturn(IP_ADDR);

            when(audited.action()).thenReturn(AdminAuditAction.USER_LOCK);
            when(audited.target()).thenReturn(AdminAuditTargetType.USER);
            when(audited.targetId()).thenReturn("#targetUserId");
            when(audited.detail()).thenReturn("'Reason: ' + #reason");

            Object result = aspect.around(joinPoint, audited);

            assertThat(result).isEqualTo("TargetMethodResult");
            verify(auditService).record(
                    ADMIN_ID,
                    AdminAuditAction.USER_LOCK,
                    AdminAuditTargetType.USER,
                    "100",
                    "Reason: Spam content",
                    IP_ADDR
            );
            verify(transactionManager).commit(transactionStatus);
            verify(transactionManager, never()).rollback(transactionStatus);
        }

        @Test
        @DisplayName("Rollback transaction and rethrow exception when target method throws RuntimeException")
        void around_WhenTargetThrowsRuntimeException_ShouldRollbackAndRethrow() throws Throwable {
            RuntimeException originalException = new IllegalStateException("Business logic failed");
            when(joinPoint.proceed()).thenThrow(originalException);

            assertThatThrownBy(() -> aspect.around(joinPoint, audited))
                    .isSameAs(originalException);

            verify(auditService, never()).record(any(), any(), any(), any(), any(), any());
            verify(transactionManager).rollback(transactionStatus);
            verify(transactionManager, never()).commit(transactionStatus);
        }

        @Test
        @DisplayName("Rollback transaction and rethrow original checked exception when target method throws checked exception")
        void around_WhenTargetThrowsCheckedException_ShouldRollbackAndRethrowCheckedException() throws Throwable {
            IOException originalCheckedException = new IOException("Disk failure");
            when(joinPoint.proceed()).thenThrow(originalCheckedException);

            assertThatThrownBy(() -> aspect.around(joinPoint, audited))
                    .isInstanceOf(IOException.class)
                    .hasMessage("Disk failure");

            verify(auditService, never()).record(any(), any(), any(), any(), any(), any());
            verify(transactionManager).rollback(transactionStatus);
            verify(transactionManager, never()).commit(transactionStatus);
        }

        @Test
        @DisplayName("Rollback transaction and throw exception when auditService.record fails")
        void around_WhenAuditServiceFails_ShouldRollbackAndThrow() throws Throwable {
            when(joinPoint.proceed()).thenReturn("OK");

            when(currentAdmin.id()).thenReturn(ADMIN_ID);
            when(currentAdmin.ip()).thenReturn(IP_ADDR);

            when(audited.action()).thenReturn(AdminAuditAction.USER_UNLOCK);
            when(audited.target()).thenReturn(AdminAuditTargetType.USER);
            when(audited.targetId()).thenReturn("");
            when(audited.detail()).thenReturn("");

            doThrow(new RuntimeException("Database error saving audit log"))
                    .when(auditService).record(any(), any(), any(), any(), any(), any());

            assertThatThrownBy(() -> aspect.around(joinPoint, audited))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error saving audit log");

            verify(transactionManager).rollback(transactionStatus);
        }

        @Test
        @DisplayName("Catch SpEL evaluation exception, fallback to null, and record log normally")
        void around_WhenSpelFails_ShouldFallbackToNullAndRecordSuccessfully() throws Throwable {
            when(joinPoint.proceed()).thenReturn("TargetMethodResult");
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.toShortString()).thenReturn("MockService.doSomething(..)");
            when(methodSignature.getParameterNames()).thenReturn(new String[]{});
            when(joinPoint.getArgs()).thenReturn(new Object[]{});

            when(currentAdmin.id()).thenReturn(ADMIN_ID);
            when(currentAdmin.ip()).thenReturn(IP_ADDR);

            when(audited.action()).thenReturn(AdminAuditAction.USER_LOCK);
            when(audited.target()).thenReturn(AdminAuditTargetType.USER);
            when(audited.targetId()).thenReturn("#nonExistentProperty.subField");
            when(audited.detail()).thenReturn(null);

            Object result = aspect.around(joinPoint, audited);

            assertThat(result).isEqualTo("TargetMethodResult");
            verify(auditService).record(
                    ADMIN_ID,
                    AdminAuditAction.USER_LOCK,
                    AdminAuditTargetType.USER,
                    null,
                    null,
                    IP_ADDR
            );
            verify(transactionManager).commit(transactionStatus);
        }
    }
}
