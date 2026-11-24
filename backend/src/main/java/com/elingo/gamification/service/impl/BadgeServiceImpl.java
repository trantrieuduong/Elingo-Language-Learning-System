package com.elingo.gamification.service.impl;

import com.elingo.gamification.dto.request.BadgeRequest;
import com.elingo.gamification.dto.response.AdminBadgeResponse;
import com.elingo.common.enums.SystemFileModule;
import com.elingo.common.event.FileDeletedEvent;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.file.util.FileKey;
import com.elingo.file.util.MediaKeyResolver;
import com.elingo.gamification.catalog.BadgeCatalog;
import com.elingo.gamification.dto.response.UserBadgeEarnedResponse;
import com.elingo.gamification.entity.Badge;
import com.elingo.gamification.entity.BadgeConditionType;
import com.elingo.gamification.entity.UserBadge;
import com.elingo.gamification.mapper.BadgeMapper;
import com.elingo.gamification.repository.BadgeRepository;
import com.elingo.gamification.repository.UserBadgeRepository;
import com.elingo.gamification.service.BadgeService;
import com.elingo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
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
    private final BadgeRepository badgeRepository;
    private final BadgeCatalog badgeCatalog;
    private final MediaKeyResolver mediaKeyResolver;
    private final ApplicationEventPublisher publisher;

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

    @Override
    @Transactional(readOnly = true)
    public List<AdminBadgeResponse> getAllBadgesForAdmin(BadgeConditionType conditionType) {
        if (conditionType != null) {
            return badgeMapper.toAdminBadgeResponseList(badgeRepository.findAllByConditionType(conditionType));
        }
        return badgeMapper.toAdminBadgeResponseList(badgeRepository
                .findAll(Sort.by(Sort.Order.asc("conditionType"), Sort.Order.asc("conditionValue"))));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminBadgeResponse getBadgeByIdForAdmin(Long id) {
        return badgeMapper.toAdminBadgeResponse(badgeRepository.findById(id)
                .orElseThrow(() -> new AppException(AppError.BADGE_NOT_FOUND)));
    }

    @Override
    @Transactional
    public AdminBadgeResponse createBadge(BadgeRequest request) {
        if (badgeRepository.existsByCode(request.code())) {
            throw new AppException(AppError.BADGE_CODE_EXISTED);
        }

        var icon = mediaKeyResolver.forCreate(request.iconUrl(), SystemFileModule.BADGES);

        Badge badge = badgeMapper.toBadge(request);
        badge.setIconUrl(icon.key());

        Badge saved = badgeRepository.save(badge);
        mediaKeyResolver.publish(icon);
        badgeCatalog.reload();

        log.info("Badge created id={} code={} conditionType={} conditionValue={}",
                saved.getId(), saved.getCode(), saved.getConditionType(), saved.getConditionValue());
        return badgeMapper.toAdminBadgeResponse(saved);
    }

    @Override
    @Transactional
    public AdminBadgeResponse updateBadge(Long id, BadgeRequest request) {
        Badge badge = badgeRepository.findById(id)
                .orElseThrow(() -> new AppException(AppError.BADGE_NOT_FOUND));

        if (!badge.getCode().equals(request.code()) && badgeRepository.existsByCodeAndIdNot(request.code(), id)) {
            throw new AppException(AppError.BADGE_CODE_EXISTED);
        }

        var icon = mediaKeyResolver.forUpdate(request.iconUrl(), badge.getIconUrl(), SystemFileModule.BADGES);

        badgeMapper.updateBadge(badge, request);
        badge.setIconUrl(icon.key());

        Badge updated = badgeRepository.save(badge);
        mediaKeyResolver.publish(icon);
        badgeCatalog.reload();

        log.info("Badge updated id={} code={}", id, updated.getCode());
        return badgeMapper.toAdminBadgeResponse(updated);
    }

    @Override
    @Transactional
    public void deleteBadge(Long id) {
        Badge badge = badgeRepository.findById(id)
                .orElseThrow(() -> new AppException(AppError.BADGE_NOT_FOUND));

        if (userBadgeRepository.existsByBadgeId(id)) {
            throw new AppException(AppError.BADGE_IN_USE);
        }

        String iconKey = badge.getIconUrl();
        badgeRepository.delete(badge);

        if (iconKey != null && FileKey.isPermanentKey(iconKey)) {
            publisher.publishEvent(new FileDeletedEvent(List.of(iconKey)));
        }

        badgeCatalog.reload();
        log.info("Badge deleted id={} code={}", id, badge.getCode());
    }

    @Override
    public List<BadgeConditionType> getConditionTypes() {
        return List.of(BadgeConditionType.values());
    }
}
