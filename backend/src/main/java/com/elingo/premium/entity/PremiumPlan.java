package com.elingo.premium.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import com.elingo.common.entity.BaseEntity;

@Entity
@Table(name = "premium_plans")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PremiumPlan extends BaseEntity {
    @Column(nullable = false, length = 150)
    String name;

    @Column(precision = 12, scale = 2, nullable = false)
    BigDecimal price;

    @Column(length = 10, nullable = false)
    @Builder.Default
    String currency = "VND";

    @Column(name = "duration_days", nullable = false)
    Integer durationDays;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    Boolean isActive = true;
}
