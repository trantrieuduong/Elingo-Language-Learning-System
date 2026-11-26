package com.elingo.gamification.catalog;

import com.elingo.gamification.entity.Badge;
import com.elingo.gamification.entity.BadgeConditionType;
import com.elingo.gamification.repository.BadgeRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "BADGE-CATALOG")
public class BadgeCatalog {

    private final BadgeRepository badgeRepository;

    @Getter
    @SuppressWarnings("java:S3077") // Các collection bất biến chỉ gán lại reference khi reload()
    private volatile List<Badge> allBadges = List.of();
    @SuppressWarnings("java:S3077")
    private volatile Map<BadgeConditionType, List<Badge>> badgesByType = Map.of();
    @SuppressWarnings("java:S3077")
    private volatile Map<Long, Badge> badgeById = Map.of();
    @SuppressWarnings("java:S3077")
    private volatile Map<String, Badge> badgeByCode = Map.of();

    @PostConstruct
    public void reload() {
        List<Badge> loaded = badgeRepository.findAll(
                Sort.by(Sort.Order.asc("conditionType"), Sort.Order.asc("conditionValue"))
        );

        Map<BadgeConditionType, List<Badge>> byType = loaded.stream()
                .collect(Collectors.groupingBy(
                        Badge::getConditionType,
                        () -> new EnumMap<>(BadgeConditionType.class),
                        Collectors.toUnmodifiableList()
                ));

        Map<Long, Badge> byId = loaded.stream()
                .collect(Collectors.toUnmodifiableMap(Badge::getId, Function.identity()));

        Map<String, Badge> byCode = loaded.stream()
                .collect(Collectors.toUnmodifiableMap(Badge::getCode, Function.identity()));

        this.allBadges = List.copyOf(loaded);
        this.badgesByType = Collections.unmodifiableMap(byType);
        this.badgeById = byId;
        this.badgeByCode = byCode;

        log.info("Badge catalog loaded size={}", allBadges.size());
    }

    public List<Badge> getBadgesByType(BadgeConditionType conditionType) {
        if (conditionType == null) return List.of();
        return badgesByType.getOrDefault(conditionType, List.of());
    }

    public Optional<Badge> findById(Long id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(badgeById.get(id));
    }

    public Optional<Badge> findByCode(String code) {
        if (code == null) return Optional.empty();
        return Optional.ofNullable(badgeByCode.get(code));
    }
}
