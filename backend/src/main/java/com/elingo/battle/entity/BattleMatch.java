package com.elingo.battle.entity;

import com.elingo.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.LocalDateTime;
import com.elingo.common.entity.BaseEntity;

@Entity
@Table(
        name = "battle_matches",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_battle_match_room_code", columnNames = "room_code")
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BattleMatch extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    BattleMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false)
    BattleMatchType matchType;

    @Column(name = "room_code", length = 20)
    String roomCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    BattleMatchStatus status = BattleMatchStatus.WAITING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    User winner;

    @Column(name = "total_rounds", nullable = false)
    @Builder.Default
    Short totalRounds = 10;

    @Column(name = "started_at")
    LocalDateTime startedAt;

    @Column(name = "finished_at")
    LocalDateTime finishedAt;
}
