package com.elingo.vocabulary.repository;

import com.elingo.vocabulary.entity.Deck;
import com.elingo.vocabulary.entity.DeckStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeckRepository extends JpaRepository<Deck, Long> {
    @Query("""
            SELECT d FROM Deck d
            LEFT JOIN FETCH d.tags
            LEFT JOIN FETCH d.cefrLevels
            WHERE d.status = :status
            ORDER BY d.publishedAt DESC
            """)
    List<Deck> findAllByStatusWithDetails(DeckStatus status);
}

