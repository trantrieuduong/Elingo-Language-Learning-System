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

    interface TopicUnlearnedCardCount {
        Long getTopicId();
        Integer getUnlearnedCardCount();
    }

    @Query("""
        SELECT c.topic.id AS topicId, COUNT(c) AS unlearnedCardCount
        FROM Card c
        LEFT JOIN UserCardState ucs ON ucs.card.id = c.id AND ucs.user.id = :userId
        WHERE c.deck.id = :deckId
        AND c.deck.status = DeckStatus.PUBLISHED
        AND (
            ucs.id IS NULL
            OR (ucs.srsNextReviewAt IS NULL AND ucs.flagsHidden = false)
        )
        GROUP BY c.topic.id
    """)
    List<TopicUnlearnedCardCount> countUnlearnedCardsByDeckIdGroupByTopic(Long deckId, Long userId);
}
