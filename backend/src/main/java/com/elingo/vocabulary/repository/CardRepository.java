package com.elingo.vocabulary.repository;

import com.elingo.vocabulary.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByTopicId(Long topicId);

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
}
