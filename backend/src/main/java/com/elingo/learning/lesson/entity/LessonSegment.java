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

import com.elingo.common.entity.BaseEntity;

@Entity
@Table(
        name = "lesson_segments",
        indexes = {
                @Index(name = "idx_segment_lesson", columnList = "lesson_id")
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonSegment extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Lesson lesson;

    @Column(name = "start_ms", nullable = false)
    Integer startMs;

    @Column(name = "end_ms", nullable = false)
    Integer endMs;

    @Column(name = "transcript_original", columnDefinition = "TEXT", nullable = false)
    String transcriptOriginal;

    @Column(name = "transcript_normalized", columnDefinition = "TEXT", nullable = false)
    String transcriptNormalized;

    @Column(columnDefinition = "TEXT")
    String translation;
}
