package com.elingo.learning.flashcard.repository;

import com.elingo.learning.flashcard.entity.UserCardState;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserCardStateRepository extends JpaRepository<UserCardState, Long> {
    interface CardStateView {
        Long getCardId();
        Short getSrsLastGrade();
        Boolean getFlagsStarred();
        Boolean getFlagsHidden();
    }

    interface LearnedCount {
        Long getRefId();
        Long getLearnedCount();
    }

    @Query("""
            select s from UserCardState s
            join fetch s.card c
            where s.user.id = :userId
              and s.flagsHidden = false
              and s.srsNextReviewAt is not null and s.srsNextReviewAt <= :now
            order by s.srsNextReviewAt asc
            """)
    List<UserCardState> findDue(@Param("userId") Long userId,
                                @Param("now") LocalDateTime now,
                                Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from UserCardState s
            where s.user.id = :userId and s.card.id = :cardId
            """)
    Optional<UserCardState> findForUpdate(@Param("userId") Long userId,
                                          @Param("cardId") Long cardId);

    @Query("""
    select s.card.id as cardId, s.srsLastGrade as srsLastGrade,
           s.flagsStarred as flagsStarred, s.flagsHidden as flagsHidden
    from UserCardState s
    where s.user.id = :userId and s.card.id in :cardIds
    """)
    List<CardStateView> findStateViews(@Param("userId") Long userId,
                                       @Param("cardIds") Collection<Long> cardIds);

    @Query("""
    select c.deck.id as refId, count(s.id) as learnedCount
    from UserCardState s join s.card c
    where s.user.id = :userId
      and s.flagsHidden = true
      and s.srsNextReviewAt is not null
      and c.deck.id in :deckIds
    group by c.deck.id
    """) // idx_due_review
    List<LearnedCount> countLearnedByDecks(@Param("userId") Long userId,
                                           @Param("deckIds") Collection<Long> deckIds);

    @Query("""
    select c.topic.id as refId, count(s.id) as learnedCount
    from UserCardState s join s.card c
    where s.user.id = :userId
      and s.flagsHidden = true
      and s.srsNextReviewAt is not null
      and c.deck.id in :deckIds
    group by c.topic.id
    """)
    List<LearnedCount> countLearnedByTopics(@Param("userId") Long userId,
                                            @Param("topicIds") Collection<Long> topicIds);
}
