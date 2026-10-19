package com.elingo.vocabulary.repository;

import com.elingo.vocabulary.entity.Deck;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeckRepository extends JpaRepository<Deck, Long>, JpaSpecificationExecutor<Deck> {
    long countByOwnerId(Long ownerId);

    interface DeckAccessView {
        Long getId();
        DeckStatus getStatus();
        OwnerType getOwnerType();
        Long getOwnerId();
        Boolean getIsPremium();
    }

    @Query("""
    select d.id as id, d.status as status, d.ownerType as ownerType,
           d.owner.id as ownerId, d.isPremium as isPremium
    from Deck d where d.id = :deckId
    """)
    @EntityGraph(attributePaths = "owner") // @ManyToOne only
    Optional<DeckAccessView> findAccessViewById(@Param("deckId") Long Id);

    @Query("""
    select d.id as id, d.status as status, d.ownerType as ownerType,
           d.owner.id as ownerId, d.isPremium as isPremium
    from Topic t join t.deck d where t.id = :topicId
    """)
    Optional<DeckAccessView> findAccessViewByTopicId(@Param("topicId") Long topicId);

    @Query("""
    select d.id as id, d.status as status, d.ownerType as ownerType,
           d.owner.id as ownerId, d.isPremium as isPremium
    from Card c join c.deck d where c.id = :cardId
    """)
    Optional<DeckAccessView> findAccessViewByCardId(@Param("cardId") Long cardId);

    @Modifying
    @Query("update Deck d set d.cardCount = d.cardCount + :delta where d.id = :id")
    void adjustCardCount(@Param("id") Long id, @Param("delta") int delta);

    @Modifying
    @Query("update Deck d set d.topicCount = d.topicCount + :delta where d.id = :id")
    void adjustTopicCount(@Param("id") Long id, @Param("delta") int delta);
}

