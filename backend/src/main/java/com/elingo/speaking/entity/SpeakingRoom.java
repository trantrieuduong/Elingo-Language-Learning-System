package com.elingo.speaking.entity;

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
        name = "speaking_rooms",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_speaking_room_code", columnNames = "room_code")
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SpeakingRoom extends BaseEntity {
    @Column(name = "room_code", length = 20, nullable = false)
    String roomCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    User host;

    @Column
    String topic;

    @Column(name = "max_participants", nullable = false)
    @Builder.Default
    Short maxParticipants = 4;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    SpeakingRoomStatus status = SpeakingRoomStatus.WAITING;

    @Column(name = "started_at")
    LocalDateTime startedAt;

    @Column(name = "ended_at")
    LocalDateTime endedAt;
}
