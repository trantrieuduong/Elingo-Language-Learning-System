package com.elingo.battle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "battle_answers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_question_participant", columnNames = {"question_id", "participant_id"})
        }
)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BattleAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    BattleQuestion question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    BattleParticipant participant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    BattleQuestionOption selectedOption;

    @Column(name = "typed_answer")
    String typedAnswer;

    @Column(name = "is_correct", nullable = false)
    Boolean isCorrect;

    @Column(name = "response_time_ms", nullable = false)
    Integer responseTimeMs;

    @CreationTimestamp
    @Column(name = "answered_at", updatable = false)
    LocalDateTime answeredAt;
}
