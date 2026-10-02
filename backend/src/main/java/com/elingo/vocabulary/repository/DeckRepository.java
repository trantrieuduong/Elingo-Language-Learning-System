package com.elingo.vocabulary.repository;

import com.elingo.vocabulary.entity.Deck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
                    SELECT COUNT(DISTINCT d) FROM Deck d
                    LEFT JOIN d.cefrLevels cl
                    LEFT JOIN d.tags t
                    WHERE d.status = 'PUBLISHED'
                      AND (:cefrCode IS NULL OR EXISTS (
                              SELECT 1 FROM d.cefrLevels cl2 WHERE cl2.code = :cefrCode))
                      AND (:tagCode  IS NULL OR EXISTS (
                              SELECT 1 FROM d.tags t2  WHERE t2.code  = :tagCode))
                      AND (:keyword  IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
                    """
    )//query chưa tối ưu
    Page<Deck> findPublishedWithFilters(
            @Param("cefrCode") String cefrCode,
            @Param("tagCode") String tagCode,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}

