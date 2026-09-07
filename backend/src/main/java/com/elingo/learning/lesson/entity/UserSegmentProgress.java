package com.elingo.learning.lesson.entity;

import com.elingo.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_segment_progress",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_segment", columnNames = {"user_id", "segment_id"})
        }
)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserSegmentProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    LessonSegment segment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    User user;

    @Column(name = "dictation_attempt_count", nullable = false)
    @Builder.Default
    Integer dictationAttemptCount = 0;

    @Column(name = "dictation_best_score", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    BigDecimal dictationBestScore = BigDecimal.ZERO;

    @Column(name = "dictation_hint_used_count", nullable = false)
    @Builder.Default
    Integer dictationHintUsedCount = 0;

    @Column(name = "shadowing_attempt_count", nullable = false)
    @Builder.Default
    Integer shadowingAttemptCount = 0;

    @Column(name = "shadowing_best_score", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    BigDecimal shadowingBestScore = BigDecimal.ZERO;

    @Column(name = "shadowing_latest_audio_url", length = 500)
    String shadowingLatestAudioUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
