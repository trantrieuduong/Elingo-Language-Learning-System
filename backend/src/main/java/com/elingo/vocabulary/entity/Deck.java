package com.elingo.vocabulary.entity;

import com.elingo.user.entity.User;
import jakarta.persistence.*;
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
        name = "decks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_deck_slug", columnNames = "slug")
        },
        indexes = {
                @Index(name = "idx_deck_owner", columnList = "owner_type, owner_id")
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Deck extends BaseEntity {
    @Column(nullable = false)
    String title;

    @Column(nullable = false)
    String slug;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "is_premium", nullable = false)
    @Builder.Default
    Boolean isPremium = false;

    @Column(name = "cover_image_url", length = 500)
    String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    DeckStatus status = DeckStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false)
    @Builder.Default
    OwnerType ownerType = OwnerType.SYSTEM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    User owner;

    @Column(name = "topic_count", nullable = false)
    @Builder.Default
    Integer topicCount = 0;

    @Column(name = "card_count", nullable = false)
    @Builder.Default
    Integer cardCount = 0;

    @Column(name = "published_at")
    LocalDateTime publishedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "deck_tags",
            joinColumns = @JoinColumn(name = "deck_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    Set<Tag> tags = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "deck_cefr_levels",
            joinColumns = @JoinColumn(name = "deck_id"),
            inverseJoinColumns = @JoinColumn(name = "cefr_level_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    Set<CefrLevel> cefrLevels = new HashSet<>();
}
