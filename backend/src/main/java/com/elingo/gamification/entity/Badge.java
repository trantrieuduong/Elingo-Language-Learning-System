package com.elingo.gamification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import com.elingo.common.entity.BaseEntity;

@Entity
@Table(
        name = "badges",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_badge_code", columnNames = "code")
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Badge extends BaseEntity {
    @Column(nullable = false, length = 100)
    String code;

    @Column(nullable = false, length = 150)
    String name;

    @Column(length = 500)
    String description;

    @Column(name = "icon_url", length = 500)
    String iconUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false)
    BadgeConditionType conditionType;

    @Column(name = "condition_value", nullable = false)
    Integer conditionValue;
}
