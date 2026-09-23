package com.elingo.gamification.entity;

import com.elingo.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import com.elingo.common.entity.BaseEntity;

@Entity
@Table(
        name = "leaderboard_snapshots",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_period_user", columnNames = {"period_type", "period_key", "user_id"})
        },
        indexes = {
                @Index(name = "idx_period_rank", columnList = "period_type, period_key, `rank`")
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LeaderboardSnapshot extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false)
    LeaderboardPeriodType periodType;

    @Column(name = "period_key", length = 20, nullable = false)
    String periodKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    User user;

    @Column(name = "`rank`", nullable = false)
    Integer rank;

    @Column(name = "total_xp", nullable = false)
    Integer totalXp;

    @CreationTimestamp
    @Column(name = "generated_at", updatable = false)
    LocalDateTime generatedAt;
}
