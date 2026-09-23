package com.elingo.learning.lesson.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import com.elingo.common.entity.BaseEntity;

@Entity
@Table(
        name = "segment_word_accuracy",
        indexes = {
                @Index(name = "idx_word", columnList = "user_segment_progress_id, word")
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SegmentWordAccuracy extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_segment_progress_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    UserSegmentProgress userSegmentProgress;

    @Column(nullable = false, length = 100)
    String word;

    @Column(precision = 5, scale = 2, nullable = false)
    BigDecimal accuracy;
}
