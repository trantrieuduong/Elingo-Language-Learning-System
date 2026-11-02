package com.elingo.battle.entity;

import com.elingo.user.entity.User;
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
        name = "battle_participants",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_match_user", columnNames = {"match_id", "user_id"})
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BattleParticipant extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    BattleMatch match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    User user;

    @Column(nullable = false)
    @Builder.Default
    Integer score = 0;

    @Column(name = "correct_count", nullable = false)
    @Builder.Default
    Integer correctCount = 0;

    @Column(name = "is_winner", nullable = false)
    @Builder.Default
    Boolean isWinner = false;
}
