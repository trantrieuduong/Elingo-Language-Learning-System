package com.elingo.learning.flashcard.repository;

import com.elingo.learning.flashcard.entity.UserCardState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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
}
