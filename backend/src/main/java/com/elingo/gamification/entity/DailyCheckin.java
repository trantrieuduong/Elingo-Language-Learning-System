package com.elingo.gamification.entity;

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

import java.time.LocalDate;
import com.elingo.common.entity.BaseEntity;

@Entity
@Table(
        name = "daily_checkins",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_checkin_date", columnNames = {"user_id", "checkin_date"})
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DailyCheckin extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    User user;

    @Column(name = "checkin_date", nullable = false)
    LocalDate checkinDate;

    @Column(name = "streak_day_no", nullable = false)
    Integer streakDayNo;

    @Column(name = "xp_rewarded", nullable = false)
    @Builder.Default
    Integer xpRewarded = 0;
}
