package com.elingo.learning.lesson.entity;

import com.elingo.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import com.elingo.common.entity.BaseEntity;

@Entity
@Table(
        name = "user_lesson_progress",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_lesson", columnNames = {"user_id", "lesson_id"})
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserLessonProgress extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "dictation_status", nullable = false)
    @Builder.Default
    ProgressStatus dictationStatus = ProgressStatus.NOT_STARTED;

    @Column(name = "dictation_progress_pct", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    BigDecimal dictationProgressPct = BigDecimal.ZERO;

    @Column(name = "dictation_last_start_ms")
    Integer dictationLastStartMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "shadowing_status", nullable = false)
    @Builder.Default
    ProgressStatus shadowingStatus = ProgressStatus.NOT_STARTED;

    @Column(name = "shadowing_progress_pct", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    BigDecimal shadowingProgressPct = BigDecimal.ZERO;

    @Column(name = "shadowing_last_start_ms")
    Integer shadowingLastStartMs;
}
