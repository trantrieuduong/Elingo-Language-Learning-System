package com.elingo.common.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "system_configs",
        uniqueConstraints = @UniqueConstraint(name = "uk_system_config_key", columnNames = "config_key")
)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SystemConfig extends BaseEntity {

    @Column(name = "config_key", length = 150, nullable = false)
    String configKey;

    @Column(name = "config_value", columnDefinition = "TEXT", nullable = false)
    String configValue;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "value_type", length = 20, nullable = false)
    ConfigValueType valueType;

    @Column(name = "is_secret", nullable = false)
    @Builder.Default
    Boolean isSecret = false;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "module", length = 50, nullable = false)
    ConfigModule module;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Column(name = "updated_by")
    Long updatedBy;
}
