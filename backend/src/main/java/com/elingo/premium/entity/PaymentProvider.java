package com.elingo.premium.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import com.elingo.common.entity.BaseEntity;

@Entity
@Table(
        name = "payment_providers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_provider_code", columnNames = "code")
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentProvider extends BaseEntity {
    @Column(length = 50, nullable = false)
    String code;

    @Column(length = 150, nullable = false)
    String name;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    Boolean isActive = true;
}
