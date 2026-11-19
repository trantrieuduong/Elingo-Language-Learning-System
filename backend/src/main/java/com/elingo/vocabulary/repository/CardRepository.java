package com.elingo.vocabulary.repository;

import com.elingo.vocabulary.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    @Query("""
        select distinct c from Card c
        left join fetch c.phonetics
        where c.topic.id = :topicId
        order by c.order asc, c.id asc
    """)
    List<Card> findByTopicIdWithPhonetics(@Param("topicId") Long topicId);

    long countByTopicId(Long topicId);

    Optional<Card> findFirstByTopicIdOrderByOrderDesc(Long topicId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Card c set c.deck = null, c.topic = null where c.topic.id = :topicId")
    int detachByTopicId(@Param("topicId") Long topicId);

    @Query(value = """
        SELECT COUNT(*) FROM cards c
        WHERE c.deck_id IS NULL AND c.topic_id IS NULL
          AND NOT EXISTS (SELECT 1 FROM user_card_states s WHERE s.card_id = c.id)
        """, nativeQuery = true)
    long countSafeToDeleteOrphans();

    @Query(value = """
        SELECT c.id FROM cards c
        WHERE c.deck_id IS NULL AND c.topic_id IS NULL
          AND NOT EXISTS (SELECT 1 FROM user_card_states s WHERE s.card_id = c.id)
        ORDER BY c.id LIMIT :limit
        """, nativeQuery = true)
    List<Long> findSafeToDeleteOrphanIds(@Param("limit") int limit);

    @Query("""
        select distinct c from Card c
        left join fetch c.phonetics
        where c.id in :ids
        order by c.id asc
    """)
    List<Card> findAllByIdWithPhonetics(@Param("ids") Collection<Long> ids);

    @Query(value = "select c from Card c where c.deck is null and c.topic is null",
           countQuery = "select count(c) from Card c where c.deck is null and c.topic is null")
    Page<Card> findOrphanedCards(Pageable pageable);

    @Query("select count(c) from Card c where c.deck is null and c.topic is null")
    long countOrphanedCards();
}
