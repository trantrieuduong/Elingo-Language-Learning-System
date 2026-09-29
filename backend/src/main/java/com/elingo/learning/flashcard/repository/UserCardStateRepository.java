package com.elingo.learning.flashcard.repository;

import com.elingo.learning.flashcard.entity.UserCardState;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserCardStateRepository extends JpaRepository<UserCardState, Long> {
    @Query("""
            SELECT u FROM UserCardState u
            JOIN FETCH u.card c
            WHERE u.card.id = :cardId
              AND u.user.id = :userId
            """)
    Optional<UserCardState> findByCardIdAndUserId(Long cardId, Long userId);

    @Query("""
            SELECT u FROM UserCardState u
            JOIN FETCH u.card c
            JOIN FETCH u.deck d
            WHERE u.user.id = :userId
              AND u.srsNextReviewAt IS NOT NULL
              AND u.srsNextReviewAt <= :now
              AND u.flagsHidden = false
              AND d.status = 'PUBLISHED'
            ORDER BY u.srsNextReviewAt ASC
            """)
    List<UserCardState> findCardsForReview(Long userId, LocalDateTime now, Pageable pageable);
}
