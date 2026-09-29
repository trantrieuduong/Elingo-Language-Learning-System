package com.elingo.vocabulary.repository;

import com.elingo.vocabulary.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findAllByDeckIdOrderByOrderAsc(Long deckId);

    @Query("""
            SELECT t FROM Topic t
            JOIN FETCH t.deck d
            WHERE t.id = :topicId
            """)
    Optional<Topic> findByIdWithDeck(Long topicId);
}
