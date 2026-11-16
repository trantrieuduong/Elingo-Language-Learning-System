package com.elingo.gamification.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.gamification.dto.response.UserBadgeEarnedResponse;
import com.elingo.gamification.entity.UserBadge;
import com.elingo.gamification.mapper.BadgeMapper;
import com.elingo.gamification.repository.UserBadgeRepository;
import com.elingo.gamification.service.BadgeService;
import com.elingo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "BADGE-SERVICE")
public class BadgeServiceImpl implements BadgeService {

    private final UserBadgeRepository userBadgeRepository;
    private final UserRepository userRepository;
    private final BadgeMapper badgeMapper;

    @Override
    @Transactional(readOnly = true)
    public List<UserBadgeEarnedResponse> getUserBadges(Long userId) {
        if (userId == null || !userRepository.existsById(userId)) {
            throw new AppException(AppError.USER_NOT_FOUND);
        }

        List<UserBadge> userBadges = userBadgeRepository.findAllByUserIdWithBadge(userId);

        List<UserBadgeEarnedResponse> responses = userBadges.stream()
                .map(ub -> badgeMapper
                        .toBadgeResponse(ub.getBadge(), ub.getEarnedAt()))
                .toList();

        log.info("Earned badges fetched userId={} count={}", userId, responses.size());
        return responses;
    }
}
