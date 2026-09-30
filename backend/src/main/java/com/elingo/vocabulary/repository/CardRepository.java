package com.elingo.vocabulary.repository;

import com.elingo.vocabulary.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    @Query("""
              SELECT c FROM Card c
              LEFT JOIN FETCH c.phonetics
              LEFT JOIN UserCardState ucs ON ucs.card.id = c.id AND ucs.user.id = :userId
              WHERE c.topic.id = :topicId
              AND c.deck.status = DeckStatus.PUBLISHED
              AND (
                 ucs.id IS NULL
                 OR (ucs.srsNextReviewAt IS NULL AND ucs.flagsHidden = false)
              )
              ORDER BY c.order ASC
            """)
    List<Card> findAllUnlearnedCardsByTopicIdWithPhonetics(Long topicId, Long userId);

    @Query("""
                SELECT c FROM Card c
                LEFT JOIN FETCH c.phonetics
                WHERE c.id IN :ids
            """)
    List<Card> findAllWithPhoneticsByIdIn(
            List<Long> ids
    );
}
