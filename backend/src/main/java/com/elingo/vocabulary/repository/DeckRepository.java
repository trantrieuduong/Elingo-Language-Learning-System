package com.elingo.vocabulary.repository;

import com.elingo.vocabulary.entity.Deck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DeckRepository extends JpaRepository<Deck, Long> {
    @Query(
            value = """
                    SELECT d FROM Deck d
                    LEFT JOIN FETCH d.tags
                    LEFT JOIN FETCH d.cefrLevels
                    WHERE d.status = 'PUBLISHED'
                      AND (:cefrCode IS NULL OR EXISTS (
                              SELECT 1 FROM d.cefrLevels cl WHERE cl.code = :cefrCode))
                      AND (:tagCode  IS NULL OR EXISTS (
                              SELECT 1 FROM d.tags t  WHERE t.code  = :tagCode))
                      AND (:keyword  IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
                    ORDER BY d.publishedAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(d) FROM Deck d
                    WHERE d.status = 'PUBLISHED'
                      AND (:cefrCode IS NULL OR EXISTS (
                              SELECT 1 FROM d.cefrLevels cl WHERE cl.code = :cefrCode))
                      AND (:tagCode  IS NULL OR EXISTS (
                              SELECT 1 FROM d.tags t  WHERE t.code  = :tagCode))
                      AND (:keyword  IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
                    """
    )
    Page<Deck> findPublishedWithFilters(
            String cefrCode,
            String tagCode,
            String keyword,
            Pageable pageable
    );
}

