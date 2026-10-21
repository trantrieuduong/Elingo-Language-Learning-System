package com.elingo.premium.service;

public interface PremiumAccessService {
    boolean hasActivePremium(Long userId);

    /** throw AppException if the user don't have active premium plan */
    void requirePremium(Long userId);
}
