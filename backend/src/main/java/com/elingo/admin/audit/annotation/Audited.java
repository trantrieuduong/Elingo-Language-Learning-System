package com.elingo.admin.audit.annotation;

import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditTargetType;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {
    AdminAuditAction action();

    AdminAuditTargetType target();
    /** SpEL: ID đối tượng. Rỗng = không có (log lưu null). */
    String targetId() default "";

    /** SpEL: lý do hoặc giá trị trước/sau. Không đưa dữ liệu nhạy cảm vào đây. */
    String detail() default "";
}
