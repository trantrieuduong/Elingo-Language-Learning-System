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

import java.util.ArrayList;
import java.util.List;
import com.elingo.common.entity.BaseEntity;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
        name = "cards",
        indexes = {
                @Index(name = "idx_card_topic_order", columnList = "topic_id, `order`")
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Card extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Deck deck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Topic topic;

    @Column(name = "`order`", nullable = false)
    @Builder.Default
    Integer order = 0;

    @Column(nullable = false)
    String term;

    @Column(length = 50)
    String pos;

    @Column(length = 500)
    String translation;

    @Column(name = "explanation_vi", columnDefinition = "TEXT")
    String explanationVi;

    @Column(name = "explanation_en", columnDefinition = "TEXT")
    String explanationEn;

    @Column(name = "examples_vi", columnDefinition = "TEXT")
    String examplesVi;

    @Column(name = "examples_en", columnDefinition = "TEXT")
    String examplesEn;

    @Column(name = "image_url", length = 500)
    String imageUrl;

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    List<CardPhonetic> phonetics = new ArrayList<>();
}
