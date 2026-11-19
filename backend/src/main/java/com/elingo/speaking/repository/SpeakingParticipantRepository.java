package com.elingo.speaking.repository;

import com.elingo.speaking.entity.SpeakingParticipant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    boolean existsByRoomIdAndUserIdAndKickedAtIsNotNull(Long roomId, Long userId);

    Optional<SpeakingParticipant> findFirstByRoomIdAndUserIdAndKickedAtIsNotNull(Long roomId, Long userId);

    @Query("""
            select p from SpeakingParticipant p join fetch p.user
            where p.room.id = :roomId and p.kickedAt is not null
            order by p.kickedAt desc, p.id desc
            """)
    List<SpeakingParticipant> findKickedByRoomId(@Param("roomId") Long roomId);

    interface HistoryRoomRow {
        Long getRoomId();
        LocalDateTime getLastLeftAt();
    }

    // một trang các phòng (đã gộp), mới nhất trước. Ẩn phòng user đang ở
    @Query(value = """
            select r.id as roomId, max(p.leftAt) as lastLeftAt
            from SpeakingParticipant p join p.room r
            where p.user.id = :userId and p.leftAt is not null
              and lower(coalesce(r.topic, '')) like :pattern escape '!'
              and (:maxParticipants = 0 or r.maxParticipants = :maxParticipants)
              and not exists (select 1 from SpeakingParticipant a
                              where a.room = r and a.user.id = :userId and a.leftAt is null)
            group by r.id
            order by max(p.leftAt) desc, r.id desc
            """,
           countQuery = """
            select count(distinct r.id)
            from SpeakingParticipant p join p.room r
            where p.user.id = :userId and p.leftAt is not null
              and lower(coalesce(r.topic, '')) like :pattern escape '!'
              and (:maxParticipants = 0 or r.maxParticipants = :maxParticipants)
              and not exists (select 1 from SpeakingParticipant a
                              where a.room = r and a.user.id = :userId and a.leftAt is null)
            """)
    Page<HistoryRoomRow> findHistoryRooms(@Param("userId") Long userId,
                                          @Param("pattern") String pattern,
                                          @Param("maxParticipants") int maxParticipants,
                                          Pageable pageable);

    // các lượt đã rời của user trong những phòng của trang đó
    @Query("""
            select p from SpeakingParticipant p join fetch p.room r
            where p.user.id = :userId and p.leftAt is not null and r.id in :roomIds
            order by p.joinedAt asc, p.id asc
            """)
    List<SpeakingParticipant> findFinishedByUserAndRoomIds(@Param("userId") Long userId,
                                                           @Param("roomIds") Collection<Long> roomIds);

    interface HistorySummaryRow {
        Long getSessionCount();
        Long getRoomCount();
        Long getTotalSeconds();
    }

    @Query(value = """
            select count(*) as sessionCount,
                   count(distinct room_id) as roomCount,
                   coalesce(sum(timestampdiff(SECOND, joined_at, left_at)), 0) as totalSeconds
            from speaking_participants
            where user_id = :userId and left_at is not null
            """, nativeQuery = true)
    HistorySummaryRow summarize(@Param("userId") Long userId);

    interface RoomCountProjection {
        Long getRoomId();
        long getTotal();
    }
}
