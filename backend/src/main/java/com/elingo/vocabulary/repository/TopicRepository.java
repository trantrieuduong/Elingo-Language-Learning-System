package com.elingo.vocabulary.repository;

import com.elingo.vocabulary.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {
    @Query("""
        SELECT t FROM Topic t
        WHERE t.deck.id = :deckId
          AND t.isDeleted = false
        ORDER BY t.order ASC
    """)
    List<Topic> findAllByDeckIdOrderByOrderAsc(Long deckId);

    @Query("""
        SELECT t FROM Topic t
        JOIN FETCH t.deck d
        WHERE t.id = :topicId
          AND t.isDeleted = false
    """)
    Optional<Topic> findByIdWithDeck(Long topicId);

    boolean existsByDeckIdAndSlug(Long deckId, String slug);

    @Query("""
        SELECT COALESCE(MAX(t.order), -1) FROM Topic t
        WHERE t.deck.id = :deckId
          AND t.isDeleted = false
    """)
    Integer findMaxOrderByDeckId(Long deckId);

    @Modifying
    @Query("""
        UPDATE Topic t SET t.order = t.order + 1
        WHERE t.deck.id = :deckId
          AND t.isDeleted = false
          AND t.order >= :newOrder
          AND t.order < :oldOrder
    """)
    void shiftOrdersDown(Long deckId, Integer newOrder, Integer oldOrder);

    @Modifying
    @Query("""
        UPDATE Topic t SET t.order = t.order - 1
        WHERE t.deck.id = :deckId
          AND t.isDeleted = false
          AND t.order > :oldOrder
          AND t.order <= :newOrder
    """)
    void shiftOrdersUp(Long deckId, Integer oldOrder, Integer newOrder);

    @Modifying
    @Query("""
        UPDATE Topic t SET t.order = t.order - 1
        WHERE t.deck.id = :deckId
          AND t.isDeleted = false
          AND t.order > :deletedOrder
    """)
    void compactOrdersAfterDelete(Long deckId, Integer deletedOrder);

    @Modifying
    @Query("""
        UPDATE Topic t SET t.isDeleted = true
        WHERE t.deck.id = :deckId
    """)
    void softDeleteByDeckId(Long deckId);
}
