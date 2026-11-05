package com.elingo.premium.repository;

import com.elingo.premium.entity.PremiumPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PremiumPlanRepository extends JpaRepository<PremiumPlan, Long> {
    List<PremiumPlan> findAllByIsActiveTrueOrderByPriceAsc();
}
