package com.elingo.vocabulary.repository;

import com.elingo.vocabulary.entity.Card;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    @Query("""
              SELECT c FROM Card c
              LEFT JOIN FETCH c.phonetics
              LEFT JOIN UserCardState ucs ON ucs.card.id = c.id AND ucs.user.id = :userId
              WHERE c.topic.id = :topicId
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
            WHERE c.id = :cardId
              AND c.topic.id = :topicId
            """)
    Optional<Card> findByIdAndTopicIdWithPhonetics(
            Long cardId,
            Long topicId);

    @Query("""
            SELECT c FROM Card c
            WHERE c.topic.id = :topicId
              AND c.id <> :excludedCardId
            ORDER BY FUNCTION('RAND')
            """)
    List<Card> findRandomDistractorsByTopicId(
            Long topicId,
            Long excludedCardId,
            Pageable pageable);
    // FUNCTION('RAND'): Sắp xếp ngẫu nhiên các kết quả trả về (hàm thuần túy của DBMS)
    // pageable: Giới hạn số lượng bản ghi trả về

    @Query("""
            SELECT c FROM Card c
            WHERE c.deck.id = :deckId
              AND c.id <> :excludedCardId
            ORDER BY FUNCTION('RAND')
            """)
    List<Card> findRandomDistractorsByDeckId(
            Long deckId,
            Long excludedCardId,
            Pageable pageable);
}
