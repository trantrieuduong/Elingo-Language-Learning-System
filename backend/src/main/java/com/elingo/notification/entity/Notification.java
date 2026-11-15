package com.elingo.notification.entity;

import com.elingo.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import com.elingo.common.entity.BaseEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notification_user", columnList = "user_id, is_read, created_at")
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100, updatable = false)
    NotificationType type;

    @Column(nullable = false)
    String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params", columnDefinition = "json", updatable = false)
    Map<String, String> params;

    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", length = 50, updatable = false)
    NotificationRefType refType;

    @Column(name = "ref_id")
    Long refId;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    Boolean isRead = false;
}
