package com.elingo.vocabulary.support;

import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
import com.elingo.vocabulary.repository.DeckRepository;

/**
 * Hand-written test double for {@link DeckRepository.DeckAccessView}.
 *
 * <p>Mockito cannot create a mock while another stubbing is still being completed
 * (e.g. inside {@code thenReturn(Optional.of(mock))}), and stubbing every getter of a
 * mock trips strict-stubbing checks when a test only reads a few of them. A plain
 * implementation avoids both problems.
 */
public final class TestDeckAccessView implements DeckRepository.DeckAccessView {

    private final Long id;
    private final DeckStatus status;
    private final OwnerType ownerType;
    private final Long ownerId;
    private final Boolean isPremium;

    private TestDeckAccessView(Long id, DeckStatus status, OwnerType ownerType,
                               Long ownerId, Boolean isPremium) {
        this.id = id;
        this.status = status;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.isPremium = isPremium;
    }

    /** A published, free, system deck. */
    public static TestDeckAccessView published(Long id) {
        return new TestDeckAccessView(id, DeckStatus.PUBLISHED, OwnerType.SYSTEM, null, false);
    }

    /** A deck owned by {@code ownerId} in the given status. */
    public static TestDeckAccessView ownedBy(Long id, DeckStatus status, Long ownerId) {
        return new TestDeckAccessView(id, status, OwnerType.USER, ownerId, false);
    }

    /** A deck the caller can see, with an explicit visibility and premium flag. */
    public static TestDeckAccessView of(Long id, DeckStatus status, OwnerType ownerType,
                                        Long ownerId, Boolean isPremium) {
        return new TestDeckAccessView(id, status, ownerType, ownerId, isPremium);
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public DeckStatus getStatus() {
        return status;
    }

    @Override
    public OwnerType getOwnerType() {
        return ownerType;
    }

    @Override
    public Long getOwnerId() {
        return ownerId;
    }

    @Override
    public Boolean getIsPremium() {
        return isPremium;
    }
}