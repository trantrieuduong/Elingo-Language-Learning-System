package com.elingo.premium.repository;

import com.elingo.premium.entity.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentProviderRepository extends JpaRepository<PaymentProvider, Long> {
    Optional<PaymentProvider> findByCodeIgnoreCase(String code);
}

