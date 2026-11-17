package com.elingo.speaking.repository;

import com.elingo.speaking.entity.SpeakingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpeakingParticipantRepository extends JpaRepository<SpeakingParticipant, Long> {

    boolean existsByUserIdAndLeftAtIsNull(Long userId);

    @Query("select p from SpeakingParticipant p join fetch p.room where p.user.id = :userId and p.leftAt is null")
    Optional<SpeakingParticipant> findActiveByUserId(@Param("userId") Long userId);

    Optional<SpeakingParticipant> findByRoomIdAndUserIdAndLeftAtIsNull(Long roomId, Long userId);

    long countByRoomIdAndLeftAtIsNull(Long roomId);

    @Query("select p from SpeakingParticipant p join fetch p.user where p.room.id = :roomId and p.leftAt is null order by p.joinedAt asc, p.id asc")
    List<SpeakingParticipant> findActiveByRoomId(@Param("roomId") Long roomId);

    @Query("select p.room.id as roomId, count(p) as total from SpeakingParticipant p where p.room.id in :roomIds and p.leftAt is null group by p.room.id")
    List<RoomCountProjection> countActiveByRoomIds(@Param("roomIds") Collection<Long> roomIds);

    @Modifying
    @Query("update SpeakingParticipant p set p.leftAt = :now where p.room.id = :roomId and p.leftAt is null")
    int closeOpenParticipants(@Param("roomId") Long roomId, @Param("now") LocalDateTime now);

    interface RoomCountProjection {
        Long getRoomId();
        long getTotal();
    }
}
