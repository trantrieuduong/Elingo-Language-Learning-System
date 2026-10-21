package com.elingo.vocabulary.service;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.premium.service.PremiumAccessService;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
import com.elingo.vocabulary.repository.DeckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeckAccessPolicy {

    private final PremiumAccessService premiumAccessService;

    public boolean isOwner(DeckRepository.DeckAccessView deck, Long userId) {
        return deck.getOwnerType() == OwnerType.USER
                && userId != null
                && userId.equals(deck.getOwnerId());
    }

    public void assertCanView(DeckRepository.DeckAccessView deck, Long userId) {
        boolean published = deck.getStatus() == DeckStatus.PUBLISHED;

        if (!published && !isOwner(deck, userId)) {
            throw new AppException(AppError.DECK_NOT_FOUND);
        }
    }

    /**
     * Sửa/xóa deck của user: chỉ chủ deck (ownerType USER).
     * Không xem được -> 404 (không lộ deck nháp của người khác),
     * xem được nhưng không phải chủ -> 403.
     */
    public void assertCanEdit(DeckRepository.DeckAccessView deck, Long userId) {
        assertCanView(deck, userId);
        if (!isOwner(deck, userId)) {
            throw new AppException(AppError.DECK_ACCESS_DENIED);
        }
    }

    public void assertCanLearn(DeckRepository.DeckAccessView deck, Long userId) {
        assertCanView(deck, userId);
        if (Boolean.TRUE.equals(deck.getIsPremium())) {
            premiumAccessService.requirePremium(userId);
        }
    }
}
