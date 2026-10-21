package com.elingo.vocabulary.repository;

import com.elingo.vocabulary.entity.Topic;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findByDeckId(Long deckId, Sort sort);
    long countByDeckId(Long deckId);
    Optional<Topic> findFirstByDeckIdOrderByOrderDesc(Long deckId);

    @Modifying
    @Query("update Topic t set t.cardCount = t.cardCount + :delta where t.id = :id")
    void adjustCardCount(@Param("id") Long id, @Param("delta") int delta);
}
