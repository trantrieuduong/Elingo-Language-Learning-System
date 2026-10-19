package com.elingo.vocabulary.repository;

import com.elingo.vocabulary.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByTopicId(Long topicId);
    boolean existsByDeckId(Long deckId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Card c set c.deck = null, c.topic = null where c.topic.id = :topicId")
    int detachByTopicId(@Param("topicId") Long topicId);
}
