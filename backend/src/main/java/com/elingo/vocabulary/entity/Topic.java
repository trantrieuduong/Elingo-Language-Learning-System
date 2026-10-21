package com.elingo.vocabulary.entity;

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

import com.elingo.common.entity.BaseEntity;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
        name = "topics",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_topic_slug_per_deck", columnNames = {"deck_id", "slug"})
        },
        indexes = {
                @Index(name = "idx_topic_deck_order", columnList = "deck_id, `order`")
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@DynamicUpdate
public class Topic extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
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
