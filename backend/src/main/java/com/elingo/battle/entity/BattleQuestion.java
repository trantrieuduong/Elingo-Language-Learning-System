package com.elingo.battle.entity;

import com.elingo.vocabulary.entity.Card;
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
        name = "battle_questions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_match_round", columnNames = {"match_id", "round_no"})
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BattleQuestion extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    BattleMatch match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Card card;

    @Column(name = "round_no", nullable = false)
    Short roundNo;

    @Column(nullable = false)
    String term;

    @Column(name = "correct_answer", nullable = false)
    String correctAnswer;

    @Column(name = "time_limit_ms", nullable = false)
    @Builder.Default
    Integer timeLimitMs = 15000;
}
