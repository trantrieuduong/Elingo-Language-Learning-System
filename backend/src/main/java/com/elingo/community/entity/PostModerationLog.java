package com.elingo.community.entity;

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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import com.elingo.common.entity.BaseEntity;

@Entity
@Table(
        name = "post_moderation_logs",
        indexes = {
                @Index(name = "idx_log_post", columnList = "post_id, created_at")
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostModerationLog extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Post post;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false)
    ModerationActorType actorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ModerationAction action;

    @Column(name = "ai_label", length = 100)
    String aiLabel;

    @Column(name = "ai_score", precision = 5, scale = 4)
    BigDecimal aiScore;

    @Column(length = 500)
    String note;
}
