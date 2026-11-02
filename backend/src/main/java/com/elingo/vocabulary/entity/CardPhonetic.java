package com.elingo.vocabulary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name = "card_phonetics")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CardPhonetic extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Card card;

    @Column(nullable = false)
    String text;

    @Column(name = "audio_url", length = 500)
    String audioUrl;

    @Column(length = 10, nullable = false)
    @Builder.Default
    String locale = "en-US";
}
