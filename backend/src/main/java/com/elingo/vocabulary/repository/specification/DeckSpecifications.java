package com.elingo.vocabulary.repository.specification;

import com.elingo.vocabulary.entity.Deck;
import com.elingo.vocabulary.entity.DeckStatus;
import com.elingo.vocabulary.entity.OwnerType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class DeckSpecifications {

    private static final String STATUS = "status";
    private static final String TITLE = "title";
    private static final String IS_PREMIUM = "isPremium";
    private static final String OWNER_TYPE = "ownerType";
    private static final String OWNER = "owner";
    private static final String TAGS = "tags";
    private static final String CEFR_LEVELS = "cefrLevels";
    private static final String ID = "id";

    private static final char LIKE_ESCAPE = '!';

    private DeckSpecifications() {}

    public static Specification<Deck> from(DeckFilter f) {
        return Specification.allOf(
                hasStatusIn(f.statuses()),
                ownedBy(f.ownerId()),
                hasOwnerType(f.ownerType()),
                hasPremium(f.premium()),
                titleContains(f.keyword()),
                hasCefrLevels(f.cefrLevelIds()),
                hasTags(f.tagIds())
        );
    }

    public static Specification<Deck> hasStatusIn(Collection<DeckStatus> statuses) {
        return (root, query, cb) -> statuses == null || statuses.isEmpty()
                ? null
                : root.get(STATUS).in(statuses);
    }

    private static Specification<Deck> titleContains(String q) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(q)) {
                return null; // null = "không có điều kiện này"
            }
            String pattern = "%" + escapeLike(q.trim().toLowerCase()) + "%";
            return cb.like(cb.lower(root.get(TITLE)), pattern, LIKE_ESCAPE);
        };
    }

    private static Specification<Deck> hasPremium(Boolean premium) {
        return (root, query, cb) -> premium == null
                ? null
                : cb.equal(root.get(IS_PREMIUM), premium);
    }

    private static Specification<Deck> hasOwnerType(OwnerType ownerType) {
        return (root, query, cb) -> ownerType == null
                ? null
                : cb.equal(root.get(OWNER_TYPE), ownerType);
    }

    private static Specification<Deck> hasCefrLevels(Collection<Long> cefrLevelIds) {
        return matchAll(CEFR_LEVELS, cefrLevelIds);
    }

    private static Specification<Deck> hasTags(Collection<Long> tagIds) {
        return matchAll(TAGS, tagIds);
    }

    public static Specification<Deck> ownedBy(Long ownerId) {
        return (root, query, cb) -> ownerId == null
                ? null
                : cb.equal(root.get(OWNER).get(ID), ownerId);
    }

    private static Specification<Deck> matchAll(String attribute, Collection<Long> ids) {
        return (root, query, cb) -> {
            if (ids == null || ids.isEmpty()) {
                return null;
            }
            Set<Long> distinctIds = new HashSet<>(ids);       // bỏ mã trùng trước khi đếm
            Subquery<Long> sub = query.subquery(Long.class);
            Root<Deck> subDeck = sub.correlate(root);
            Join<Deck, Object> item = subDeck.join(attribute);
            sub.select(cb.countDistinct(item.get(ID))).where(item.get(ID).in(distinctIds));
            return cb.equal(sub, (long) distinctIds.size());
        };
    }

    private static String escapeLike(String value) {
        return value
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
