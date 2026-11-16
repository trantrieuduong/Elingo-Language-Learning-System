package com.elingo.gamification.service.impl;

import com.elingo.common.dto.PageResponse;
import com.elingo.common.service.RedisService;
import com.elingo.gamification.dto.response.LeaderboardItemResponse;
import com.elingo.gamification.dto.response.LeaderboardResponse;
import com.elingo.gamification.dto.response.MyLeaderboardRankResponse;
import com.elingo.gamification.entity.LeaderboardPeriodType;
import com.elingo.gamification.entity.LeaderboardSnapshot;
import com.elingo.gamification.repository.LeaderboardSnapshotRepository;
import com.elingo.gamification.repository.XpEventRepository;
import com.elingo.gamification.util.LeaderboardKeyUtils;
import com.elingo.gamification.service.LeaderboardService;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j(topic = "LEADERBOARD-SERVICE")
public class LeaderboardServiceImpl implements LeaderboardService {

    private final RedisService redis;
    private final UserRepository userRepository;
    private final LeaderboardSnapshotRepository snapshotRepository;
    private final XpEventRepository xpEventRepository;
    private final LeaderboardServiceImpl self;

    public LeaderboardServiceImpl(RedisService redis,
                                  UserRepository userRepository,
                                  LeaderboardSnapshotRepository snapshotRepository,
                                  XpEventRepository xpEventRepository,
                                  @Lazy LeaderboardServiceImpl self) {
        this.redis = redis;
        this.userRepository = userRepository;
        this.snapshotRepository = snapshotRepository;
        this.xpEventRepository = xpEventRepository;
        this.self = self;
    }

    @Override
    public void recordXpForLeaderboard(Long userId, int amount) {
        if (amount <= 0 || userId == null) return;

        LocalDate now = LocalDate.now();
        String weeklyKey = LeaderboardKeyUtils.getWeeklyRedisKey(now);
        String monthlyKey = LeaderboardKeyUtils.getMonthlyRedisKey(now);
        String userStr = String.valueOf(userId);

        try {
            redis.zIncrBy(weeklyKey, userStr, amount);
            redis.expire(weeklyKey, Duration.ofDays(14));

            redis.zIncrBy(monthlyKey, userStr, amount);
            redis.expire(monthlyKey, Duration.ofDays(60));
        } catch (RuntimeException ex) {
            log.warn("Leaderboard xp record failed amount={}", amount, ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LeaderboardResponse getRealtimeLeaderboard(Long currentUserId, LeaderboardPeriodType periodType, Pageable pageable) {
        LocalDate now = LocalDate.now();
        String redisKey = (periodType == LeaderboardPeriodType.WEEKLY)
                ? LeaderboardKeyUtils.getWeeklyRedisKey(now)
                : LeaderboardKeyUtils.getMonthlyRedisKey(now);
        String periodKey = (periodType == LeaderboardPeriodType.WEEKLY)
                ? LeaderboardKeyUtils.getWeeklyPeriodKey(now)
                : LeaderboardKeyUtils.getMonthlyPeriodKey(now);

        ensureLeaderboardWarmedUp(redisKey, periodType);

        int pageNumber = Math.max(0, pageable.getPageNumber());
        int pageSize = Math.clamp(pageable.getPageSize(), 1, 100);
        long start = (long) pageNumber * pageSize;
        long end = start + pageSize - 1;

        Long totalParticipants = redis.zCard(redisKey);
        long total = (totalParticipants != null) ? totalParticipants : 0L;

        Set<ZSetOperations.TypedTuple<String>> tuples = redis.zReverseRangeWithScores(redisKey, start, end);
        List<LeaderboardItemResponse> items = buildLeaderboardItems(tuples, start);

        int totalPages = (total == 0) ? 0 : (int) Math.ceil((double) total / pageSize);
        boolean isLast = (start + items.size()) >= total;

        PageResponse<LeaderboardItemResponse> leaderboardPage = new PageResponse<>(
                items,
                pageNumber + 1,
                pageSize,
                total,
                totalPages,
                isLast
        );

        MyLeaderboardRankResponse myRank = (currentUserId != null)
                ? getMyRankInternal(currentUserId, periodType, redisKey, periodKey)
                : null;

        return new LeaderboardResponse(periodType, periodKey, leaderboardPage, myRank);
    }

    private void ensureLeaderboardWarmedUp(String redisKey, LeaderboardPeriodType periodType) {
        try {
            if (!redis.exists(redisKey)) {
                LeaderboardServiceImpl target = (self != null) ? self : this;
                target.syncLeaderboardFromDatabase(periodType);
            }
        } catch (Exception ex) {
            log.warn("Auto-sync check on getRealtimeLeaderboard failed redisKey={}", redisKey, ex);
        }
    }

    private List<LeaderboardItemResponse> buildLeaderboardItems(Set<ZSetOperations.TypedTuple<String>> tuples, long start) {
        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = tuples.stream()
                .map(t -> Long.valueOf(t.getValue()))
                .toList();

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        int rankCounter = (int) start + 1;
        List<LeaderboardItemResponse> items = new ArrayList<>(tuples.size());

        for (var tuple : tuples) {
            items.add(toItemResponse(tuple, rankCounter++, userMap));
        }
        return items;
    }

    private LeaderboardItemResponse toItemResponse(ZSetOperations.TypedTuple<String> tuple, int rank, Map<Long, User> userMap) {
        Long uid = Long.valueOf(tuple.getValue());
        User u = userMap.get(uid);
        String username = (u != null) ? u.getUsername() : "User " + uid;
        String avatarUrl = (u != null) ? u.getAvatarUrl() : null;
        int xp = (tuple.getScore() != null) ? tuple.getScore().intValue() : 0;
        return new LeaderboardItemResponse(rank, uid, username, avatarUrl, xp);
    }

    @Override
    public MyLeaderboardRankResponse getMyRank(Long currentUserId, LeaderboardPeriodType periodType) {
        LocalDate now = LocalDate.now();
        String redisKey = (periodType == LeaderboardPeriodType.WEEKLY)
                ? LeaderboardKeyUtils.getWeeklyRedisKey(now)
                : LeaderboardKeyUtils.getMonthlyRedisKey(now);
        String periodKey = (periodType == LeaderboardPeriodType.WEEKLY)
                ? LeaderboardKeyUtils.getWeeklyPeriodKey(now)
                : LeaderboardKeyUtils.getMonthlyPeriodKey(now);

        return getMyRankInternal(currentUserId, periodType, redisKey, periodKey);
    }

    private MyLeaderboardRankResponse getMyRankInternal(Long userId, LeaderboardPeriodType periodType, String redisKey, String periodKey) {
        Long rankIndex = redis.zReverseRank(redisKey, String.valueOf(userId));
        if (rankIndex == null) {
            return new MyLeaderboardRankResponse(null, 0, 0, periodType, periodKey);
        }

        int rank = rankIndex.intValue() + 1;
        Double score = redis.zScore(redisKey, String.valueOf(userId));
        int totalXp = (score != null) ? score.intValue() : 0;

        int diffToNext = 0;
        if (rankIndex > 0) {
            Set<ZSetOperations.TypedTuple<String>> aboveTuples = redis.zReverseRangeWithScores(redisKey, rankIndex - 1, rankIndex - 1);
            if (aboveTuples != null && !aboveTuples.isEmpty()) {
                Double aboveScore = aboveTuples.iterator().next().getScore();
                if (aboveScore != null) {
                    diffToNext = Math.max(1, aboveScore.intValue() - totalXp + 1);
                }
            }
        }

        return new MyLeaderboardRankResponse(rank, totalXp, diffToNext, periodType, periodKey);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LeaderboardItemResponse> getHistoricalLeaderboard(LeaderboardPeriodType periodType, String periodKey, Pageable pageable) {
        Page<LeaderboardSnapshot> snapshots = snapshotRepository.findByPeriod(periodType, periodKey, pageable);
        return PageResponse.of(snapshots.map(s -> new LeaderboardItemResponse(
                s.getRank(),
                s.getUser().getId(),
                s.getUser().getUsername(),
                s.getUser().getAvatarUrl(),
                s.getTotalXp()
        )));
    }

    @Override
    @Transactional(readOnly = true)
    public void syncLeaderboardFromDatabase(LeaderboardPeriodType periodType) {
        LocalDate now = LocalDate.now();
        String redisKey;
        Duration ttl;
        LocalDateTime since;

        if (periodType == LeaderboardPeriodType.WEEKLY) {
            redisKey = LeaderboardKeyUtils.getWeeklyRedisKey(now);
            ttl = Duration.ofDays(14);
            since = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        } else {
            redisKey = LeaderboardKeyUtils.getMonthlyRedisKey(now);
            ttl = Duration.ofDays(60);
            since = now.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        }

        try {
            List<XpEventRepository.UserPeriodXp> list = xpEventRepository.sumXpGroupByUserSince(since);
            if (list != null && !list.isEmpty()) {
                for (var item : list) {
                    if (item.getUserId() != null && item.getTotalXp() != null) {
                        redis.zAdd(redisKey, String.valueOf(item.getUserId()), item.getTotalXp().doubleValue());
                    }
                }
                redis.expire(redisKey, ttl);
                log.info("Leaderboard synced from DB periodType={} redisKey={} count={}",
                        periodType, redisKey, list.size());
            } else {
                log.info("Leaderboard sync found no XP records periodType={} redisKey={}", periodType, redisKey);
            }
        } catch (Exception ex) {
            log.error("Failed to sync leaderboard from DB periodType={} redisKey={}", periodType, redisKey, ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void warmupLeaderboardIfAbsent(LeaderboardPeriodType periodType) {
        LocalDate now = LocalDate.now();
        String redisKey = (periodType == LeaderboardPeriodType.WEEKLY)
                ? LeaderboardKeyUtils.getWeeklyRedisKey(now)
                : LeaderboardKeyUtils.getMonthlyRedisKey(now);

        try {
            if (!redis.exists(redisKey)) {
                log.info("Leaderboard key missing in Redis, warming up periodType={} redisKey={}", periodType, redisKey);
                LeaderboardServiceImpl target = (self != null) ? self : this;
                target.syncLeaderboardFromDatabase(periodType);
            }
        } catch (Exception ex) {
            log.warn("Warmup check failed periodType={} redisKey={}", periodType, redisKey, ex);
        }
    }

    @Override
    @Transactional
    public void snapshotLeaderboard(LeaderboardPeriodType periodType, String periodKey) {
        if (snapshotRepository.existsByPeriodTypeAndPeriodKey(periodType, periodKey))
            return;

        String redisKey = (periodType == LeaderboardPeriodType.WEEKLY)
                ? LeaderboardKeyUtils.PREFIX_WEEKLY + periodKey
                : LeaderboardKeyUtils.PREFIX_MONTHLY + periodKey;

        Set<ZSetOperations.TypedTuple<String>> tuples = redis.zReverseRangeWithScores(redisKey, 0, 499);
        if (tuples == null || tuples.isEmpty()) {
            return;
        }

        List<Long> userIds = tuples.stream().map(t -> Long.valueOf(t.getValue())).toList();
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<LeaderboardSnapshot> snapshots = new ArrayList<>();
        int rank = 1;
        for (var tuple : tuples) {
            Long uid = Long.valueOf(tuple.getValue());
            User u = userMap.get(uid);
            if (u != null) {
                int totalXp = tuple.getScore() != null ? tuple.getScore().intValue() : 0;
                snapshots.add(LeaderboardSnapshot.builder()
                        .periodType(periodType)
                        .periodKey(periodKey)
                        .user(u)
                        .rank(rank++)
                        .totalXp(totalXp)
                        .build());
            }
        }

        snapshotRepository.saveAll(snapshots);
        log.info("Leaderboard snapshot created periodType={} periodKey={} count={}",
                periodType, periodKey, snapshots.size());
    }
}
