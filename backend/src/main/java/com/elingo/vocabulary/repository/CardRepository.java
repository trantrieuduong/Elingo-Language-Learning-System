package com.elingo.vocabulary.repository;

import com.elingo.vocabulary.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
              AND c.isDeleted = false
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
                AND c.isDeleted = false
            """)
    List<Card> findAllWithPhoneticsByIdIn(
            List<Long> ids
    );

    interface DeckLearnedCardCount {
        Long getDeckId();
        Integer getLearnedCardCount();
    }

    @Query("""
        SELECT c.deck.id AS deckId, COUNT(c.id) AS learnedCardCount
        FROM Card c
        JOIN UserCardState ucs ON ucs.card.id = c.id AND ucs.user.id = :userId
        WHERE c.deck.id IN :deckIds
        AND c.isDeleted = false
        AND (ucs.srsNextReviewAt IS NOT NULL OR ucs.flagsHidden = true)
        GROUP BY c.deck.id
    """)
    List<DeckLearnedCardCount> countLearnedCardsByDeckIds(List<Long> deckIds, Long userId);

    interface TopicUnlearnedCardCount {
        Long getTopicId();
        Integer getUnlearnedCardCount();
    }

    @Query("""
        SELECT c.topic.id AS topicId, COUNT(c) AS unlearnedCardCount
        FROM Card c
        LEFT JOIN UserCardState ucs ON ucs.card.id = c.id AND ucs.user.id = :userId
        WHERE c.deck.id = :deckId
        AND c.isDeleted = false
        AND c.deck.status = DeckStatus.PUBLISHED
        AND (
            ucs.id IS NULL
            OR (ucs.srsNextReviewAt IS NULL AND ucs.flagsHidden = false)
        )
        GROUP BY c.topic.id
    """)
    List<TopicUnlearnedCardCount> countUnlearnedCardsByDeckIdGroupByTopic(Long deckId, Long userId);

    @Modifying
    @Query("""
        UPDATE Card c SET c.isDeleted = true
        WHERE c.topic.id = :topicId
    """)
    void softDeleteByTopicId(Long topicId);

    @Modifying
    @Query("""
        UPDATE Card c SET c.isDeleted = true
        WHERE c.deck.id = :deckId
    """)
    void softDeleteByDeckId(Long deckId);


    @Query("""
        SELECT COALESCE(MAX(c.order), -1) FROM Card c
        WHERE c.topic.id = :topicId
          AND c.isDeleted = false
    """)
    Integer findMaxOrderByTopicId(Long topicId);

    @Modifying
    @Query("""
        UPDATE Card c SET c.order = c.order + 1
        WHERE c.topic.id = :topicId
          AND c.isDeleted = false
          AND c.order >= :newOrder
          AND c.order < :oldOrder
    """)
    void shiftOrdersDown(Long topicId, Integer newOrder, Integer oldOrder);

    @Modifying
    @Query("""
        UPDATE Card c SET c.order = c.order - 1
        WHERE c.topic.id = :topicId
          AND c.isDeleted = false
          AND c.order > :oldOrder
          AND c.order <= :newOrder
    """)
    void shiftOrdersUp(Long topicId, Integer oldOrder, Integer newOrder);

    @Modifying
    @Query("""
        UPDATE Card c SET c.order = c.order - 1
        WHERE c.topic.id = :topicId
          AND c.isDeleted = false
          AND c.order > :deletedOrder
    """)
    void compactOrdersAfterDelete(Long topicId, Integer deletedOrder);
}
