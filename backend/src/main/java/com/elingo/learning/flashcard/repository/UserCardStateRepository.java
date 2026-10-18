package com.elingo.learning.flashcard.repository;

import com.elingo.learning.flashcard.entity.UserCardState;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
      and c.deck.id in :deckIds
    group by c.deck.id
    """)
    List<LearnedCount> countLearnedByDecks(@Param("userId") Long userId,
                                           @Param("deckIds") Collection<Long> deckIds);

    @Query("""
    select c.topic.id as refId, count(s.id) as learnedCount
    from UserCardState s join s.card c
    where s.user.id = :userId
      and c.topic.id in :topicIds
    group by c.topic.id
    """)
    List<LearnedCount> countLearnedByTopics(@Param("userId") Long userId,
                                            @Param("topicIds") Collection<Long> topicIds);
}
