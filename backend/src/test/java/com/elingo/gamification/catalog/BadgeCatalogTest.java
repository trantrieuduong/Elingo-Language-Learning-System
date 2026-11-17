package com.elingo.gamification.catalog;

import com.elingo.gamification.entity.Badge;
import com.elingo.gamification.entity.BadgeConditionType;
import com.elingo.gamification.repository.BadgeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class BadgeCatalogTest {

    @Mock
    private BadgeRepository badgeRepository;

    @InjectMocks
    private BadgeCatalog badgeCatalog;

    @Nested
    @DisplayName("reload & queries")
    class CatalogTests {

        @Test
        @DisplayName("Loads badges and indexes by type, id, and code successfully")
        void reload_Success() {
            Badge streak7 = Badge.builder()
                    .code("STREAK_7_DAYS")
                    .name("7 Days")
                    .conditionType(BadgeConditionType.STREAK_DAYS)
                    .conditionValue(7)
                    .build();
            streak7.setId(1L);

            Badge streak14 = Badge.builder()
                    .code("STREAK_14_DAYS")
                    .name("14 Days")
                    .conditionType(BadgeConditionType.STREAK_DAYS)
                    .conditionValue(14)
                    .build();
            streak14.setId(2L);

            Badge xp1000 = Badge.builder()
                    .code("TOTAL_XP_1000")
                    .name("1000 XP")
                    .conditionType(BadgeConditionType.TOTAL_XP)
                    .conditionValue(1000)
                    .build();
            xp1000.setId(3L);

            when(badgeRepository.findAll(any(Sort.class))).thenReturn(List.of(streak7, streak14, xp1000));

            badgeCatalog.reload();

            assertThat(badgeCatalog.getAllBadges()).hasSize(3);

            List<Badge> streakBadges = badgeCatalog.getBadgesByType(BadgeConditionType.STREAK_DAYS);
            assertThat(streakBadges).containsExactly(streak7, streak14);

            List<Badge> xpBadges = badgeCatalog.getBadgesByType(BadgeConditionType.TOTAL_XP);
            assertThat(xpBadges).containsExactly(xp1000);

            List<Badge> battleBadges = badgeCatalog.getBadgesByType(BadgeConditionType.BATTLE_WINS);
            assertThat(battleBadges).isEmpty();

            Optional<Badge> foundById = badgeCatalog.findById(2L);
            assertThat(foundById).isPresent().contains(streak14);

            Optional<Badge> foundByCode = badgeCatalog.findByCode("STREAK_7_DAYS");
            assertThat(foundByCode).isPresent().contains(streak7);

            verify(badgeRepository).findAll(any(Sort.class));
        }

        @Test
        @DisplayName("Returns empty optional when searching by null id or code")
        void query_WithNull_ReturnsEmpty() {
            assertThat(badgeCatalog.findById(null)).isEmpty();
            assertThat(badgeCatalog.findByCode(null)).isEmpty();
            assertThat(badgeCatalog.getBadgesByType(null)).isEmpty();
        }
    }
}
