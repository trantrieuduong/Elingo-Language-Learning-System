package com.elingo.vocabulary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import com.elingo.common.entity.BaseEntity;

@Entity
@Table(
        name = "topics",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_topic_slug_per_deck", columnNames = {"deck_id", "slug"})
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Topic extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Deck deck;

    @Column(nullable = false)
    String name;

    @Column(nullable = false)
    String slug;

    @Column(name = "`order`", nullable = false)
    @Builder.Default
    Integer order = 0;

    @Column(name = "card_count", nullable = false)
    @Builder.Default
    Integer cardCount = 0;
}
