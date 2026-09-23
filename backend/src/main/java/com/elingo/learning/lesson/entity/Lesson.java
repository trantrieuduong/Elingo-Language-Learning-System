package com.elingo.learning.lesson.entity;

import com.elingo.vocabulary.entity.CefrLevel;
import com.elingo.vocabulary.entity.Tag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import com.elingo.common.entity.BaseEntity;

@Entity
@Table(
        name = "lessons",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_lesson_slug", columnNames = "slug")
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Lesson extends BaseEntity {
    @Column(nullable = false)
    String title;

    @Column(nullable = false)
    String slug;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "is_premium", nullable = false)
    @Builder.Default
    Boolean isPremium = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    LessonStatus status = LessonStatus.DRAFT;

    @Column(name = "published_at")
    LocalDateTime publishedAt;

    @Column(name = "duration_ms")
    Integer durationMs;

    @Column(name = "source_url", length = 500, nullable = false)
    String sourceUrl;

    @Column(name = "thumbnail_url", length = 500)
    String thumbnailUrl;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "lesson_tags",
            joinColumns = @JoinColumn(name = "lesson_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    Set<Tag> tags = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "lesson_cefr_levels",
            joinColumns = @JoinColumn(name = "lesson_id"),
            inverseJoinColumns = @JoinColumn(name = "cefr_level_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    Set<CefrLevel> cefrLevels = new HashSet<>();
}
