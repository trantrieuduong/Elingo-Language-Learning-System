package com.elingo.speaking.repository.specification;

import com.elingo.speaking.entity.SpeakingParticipant;
import com.elingo.speaking.entity.SpeakingRoom;
import com.elingo.speaking.entity.SpeakingRoomStatus;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class SpeakingRoomSpecifications {

    private static final char LIKE_ESCAPE = '!';

    private SpeakingRoomSpecifications() {}

    // Lobby: phòng công khai, chưa đóng, còn chỗ, + lọc tùy chọn theo chủ đề và sức chứa
    public static Specification<SpeakingRoom> openLobby(SpeakingRoomFilter f) {
        return Specification.allOf(
                isOpen(),
                isPublic(),
                hasFreeSlot(),
                topicContains(f.keyword()),
                hasCapacity(f.maxParticipants())
        );
    }

    private static Specification<SpeakingRoom> isOpen() {
        return (root, query, cb) -> root.get("status")
                .in(SpeakingRoomStatus.WAITING, SpeakingRoomStatus.ACTIVE);
    }

    private static Specification<SpeakingRoom> isPublic() {
        return (root, query, cb) -> cb.isFalse(root.get("isPrivate"));
    }

    // số thành viên đang ở (left_at IS NULL) < maxParticipants -> phòng đầy không hiện
    private static Specification<SpeakingRoom> hasFreeSlot() {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<SpeakingParticipant> p = sub.from(SpeakingParticipant.class);
            sub.select(cb.count(p)).where(cb.equal(p.get("room"), root), cb.isNull(p.get("leftAt")));
            return cb.lessThan(sub, root.get("maxParticipants").as(Long.class));
        };
    }

    private static Specification<SpeakingRoom> topicContains(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) {
                return null; // null = không có điều kiện này
            }
            String pattern = "%" + escapeLike(keyword.trim().toLowerCase()) + "%";
            return cb.like(cb.lower(root.get("topic")), pattern, LIKE_ESCAPE);
        };
    }

    private static Specification<SpeakingRoom> hasCapacity(Integer maxParticipants) {
        return (root, query, cb) -> maxParticipants == null
                ? null
                : cb.equal(root.get("maxParticipants"), maxParticipants.shortValue());
    }

    private static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
