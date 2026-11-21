package com.elingo.vocabulary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "tags",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tag_label", columnNames = "label")
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Tag extends BaseEntity {
    @Column(nullable = false, length = 150)
    String label;
}
