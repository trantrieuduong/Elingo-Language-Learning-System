package com.elingo.speaking.repository;

import com.elingo.speaking.entity.SpeakingRoom;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpeakingRoomRepository extends JpaRepository<SpeakingRoom, Long>, JpaSpecificationExecutor<SpeakingRoom> {

    boolean existsByRoomCode(String roomCode);

    @Query("select r from SpeakingRoom r join fetch r.host where r.roomCode = :roomCode")
    Optional<SpeakingRoom> findByRoomCodeWithHost(@Param("roomCode") String roomCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from SpeakingRoom r where r.roomCode = :roomCode")
    Optional<SpeakingRoom> findByRoomCodeForUpdate(@Param("roomCode") String roomCode);

    @Override
    @EntityGraph(attributePaths = "host")
    Page<SpeakingRoom> findAll(Specification<SpeakingRoom> spec, Pageable pageable);

    @Query("""
            select r.roomCode from SpeakingRoom r
            where r.status in (com.elingo.speaking.entity.SpeakingRoomStatus.WAITING,
                               com.elingo.speaking.entity.SpeakingRoomStatus.ACTIVE)
              and r.createdAt < :before
            """)
    List<String> findOpenRoomCodesCreatedBefore(@Param("before") LocalDateTime before);

    @Query("""
            select r.roomCode from SpeakingRoom r
            where r.status in (com.elingo.speaking.entity.SpeakingRoomStatus.WAITING,
                               com.elingo.speaking.entity.SpeakingRoomStatus.ACTIVE)
            """)
    List<String> findAllOpenRoomCodes();
}
