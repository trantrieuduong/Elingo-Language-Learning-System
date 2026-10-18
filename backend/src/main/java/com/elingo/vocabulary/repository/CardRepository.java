package com.elingo.vocabulary.repository;

import com.elingo.vocabulary.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByTopicId(Long topicId);

    @Query("""
                SELECT c FROM Card c
                LEFT JOIN FETCH c.phonetics
                WHERE c.id IN :ids
            """)
    List<Card> findAllWithPhoneticsByIdIn(
            List<Long> ids
    );
}
