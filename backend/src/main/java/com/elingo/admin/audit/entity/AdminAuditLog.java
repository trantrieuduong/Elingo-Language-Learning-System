package com.elingo.admin.audit.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Nhật ký thao tác của admin. Chỉ thêm, không sửa, không xóa:
 * - @Immutable: Hibernate bỏ qua mọi UPDATE lên entity này.
 * - Không có setter.
 */
@Entity
@Immutable
@Table(name = "admin_audit_logs",
        indexes = {
                @Index(name = "idx_audit_target", columnList = "target_type, target_id"),
                @Index(name = "idx_audit_admin_created", columnList = "admin_id, created_at"),
                @Index(name = "idx_audit_created", columnList = "created_at")
        })
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminAuditLog {

    public static final int TARGET_ID_MAX = 64;
    public static final int DETAIL_MAX = 1000;
    public static final int IP_MAX = 45; // đủ cho IPv6

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private AdminAuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 50)
    private AdminAuditTargetType targetType;

    /** ID đối tượng. Có thể null. */
    @Column(name = "target_id", length = TARGET_ID_MAX)
    private String targetId;

    /** Lý do, giá trị trước/sau. Không ghi dữ liệu nhạy cảm (mật khẩu, token...). */
    @Column(name = "detail", length = DETAIL_MAX)
    private String detail;

    @Column(name = "ip_address", length = IP_MAX)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
