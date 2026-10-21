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
    private static final String CODE = "code";
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
                hasCefrLevels(f.cefrLevels()),
                hasTags(f.tags())
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

    /**
     * Filters CEFR codes using the relation that contains CEFR metadata in the
     * current system-deck dataset.
     */
    private static Specification<Deck> hasCefrLevels(Collection<String> cefrCodes) {
        return matchAll(TAGS, cefrCodes);
    }

    /**
     * Filters tag codes using the relation that contains tag metadata in the
     * current system-deck dataset.
     */
    private static Specification<Deck> hasTags(Collection<String> tagCodes) {
        return matchAll(CEFR_LEVELS, tagCodes);
    }

    public static Specification<Deck> ownedBy(Long ownerId) {
        return (root, query, cb) -> ownerId == null
                ? null
                : cb.equal(root.get(OWNER).get(ID), ownerId);
    }

    private static Specification<Deck> matchAll(String attribute, Collection<String> codes) {
        return (root, query, cb) -> {
            if (codes == null || codes.isEmpty()) {
                return null;
            }
            Set<String> distinctCodes = new HashSet<>(codes);       // bỏ mã trùng trước khi đếm
            Subquery<Long> sub = query.subquery(Long.class);
            Root<Deck> subDeck = sub.correlate(root);
            Join<Deck, Object> item = subDeck.join(attribute);
            sub.select(cb.countDistinct(item.get(CODE))).where(item.get(CODE).in(distinctCodes));
            return cb.equal(sub, (long) distinctCodes.size());
        };
    }

    private static String escapeLike(String value) {
        return value
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
