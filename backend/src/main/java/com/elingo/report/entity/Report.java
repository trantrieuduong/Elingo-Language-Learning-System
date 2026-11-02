package com.elingo.report.entity;

import com.elingo.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import com.elingo.common.entity.BaseEntity;

@Entity
@Table(
        name = "reports",
        indexes = {
                @Index(name = "idx_report_target", columnList = "target_type, target_id"),
                @Index(name = "idx_report_status", columnList = "status, created_at")
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Report extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    User reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    ReportTargetType targetType;

    @Column(name = "target_id", nullable = false)
    Long targetId;

    @Column(length = 1000)
    String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    ReportStatus status = ReportStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    User resolvedBy;

    @Column(name = "resolved_at")
    LocalDateTime resolvedAt;

    @Column(name = "resolution_note", length = 500)
    String resolutionNote;
}
