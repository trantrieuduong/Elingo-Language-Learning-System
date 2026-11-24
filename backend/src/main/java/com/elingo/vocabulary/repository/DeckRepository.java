package com.elingo.vocabulary.repository;

import com.elingo.vocabulary.dto.projection.OwnerTypeCountProjection;
import com.elingo.vocabulary.dto.projection.PremiumCountProjection;
import com.elingo.vocabulary.dto.projection.StatusCountProjection;
import com.elingo.vocabulary.dto.projection.TopDeckProjection;
import com.elingo.vocabulary.entity.Deck;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeckRepository extends JpaRepository<Deck, Long>, JpaSpecificationExecutor<Deck> {
    interface DeckAccessView {
        Long getId();
        DeckStatus getStatus();
        OwnerType getOwnerType();
        Long getOwnerId();
        Boolean getIsPremium();
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Deck d where d.id = :id")
    Optional<Deck> findByIdForUpdate(@Param("id") Long id);

    long countByOwnerTypeAndOwnerId(OwnerType ownerType, Long ownerId);

    List<Deck> findAllByOwnerTypeAndOwnerId(OwnerType ownerType, Long ownerId);

    @Query("""
    select d.id as id, d.status as status, d.ownerType as ownerType,
           d.owner.id as ownerId, d.isPremium as isPremium
    from Deck d where d.slug = :slug
    """)
    @EntityGraph(attributePaths = "owner")
    Optional<DeckAccessView> findAccessViewBySlug(@Param("slug") String slug);

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

    @Query("SELECT d.status AS status, COUNT(d.id) AS countVal FROM Deck d GROUP BY d.status")
    List<StatusCountProjection> countDecksByStatus();

    @Query("SELECT d.ownerType AS ownerType, COUNT(d.id) AS countVal FROM Deck d GROUP BY d.ownerType")
    List<OwnerTypeCountProjection> countDecksByOwnerType();

    @Query("SELECT d.isPremium AS isPremium, COUNT(d.id) AS countVal FROM Deck d GROUP BY d.isPremium")
    List<PremiumCountProjection> countDecksByPremium();

    @Query(value = """
            SELECT d.id AS deckId, d.title AS title, d.cover_image_url AS coverImageUrl,
                   d.status AS status, d.owner_type AS ownerType, d.is_premium AS isPremium,
                   COUNT(DISTINCT ucs.user_id) AS learnerCount
            FROM decks d
            LEFT JOIN cards c ON c.deck_id = d.id
            LEFT JOIN user_card_states ucs ON ucs.card_id = c.id
            WHERE d.owner_type = :ownerType
            GROUP BY d.id, d.title, d.cover_image_url, d.status, d.owner_type, d.is_premium
            ORDER BY learnerCount DESC, d.id ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<TopDeckProjection> findTopDecksByOwnerType(
            @Param("ownerType") String ownerType,
            @Param("limit") int limit
    );
}

