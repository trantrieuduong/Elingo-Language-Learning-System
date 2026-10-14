package com.elingo.gamification.service.impl;

import com.elingo.common.event.LevelUpEvent;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.gamification.entity.XpEvent;
import com.elingo.gamification.entity.XpSource;
import com.elingo.gamification.repository.UserXpRepository;
import com.elingo.gamification.repository.XpEventRepository;
import com.elingo.gamification.service.LevelCatalog;
import com.elingo.gamification.service.XpService;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "XP-SERVICE")
public class XpServiceImpl implements XpService {

    private final UserXpRepository userXpRepository;
    private final XpEventRepository xpEventRepository;
    private final ApplicationEventPublisher publisher;
    private final UserRepository userRepository;
    private final LevelCatalog levelCatalog;

    @Override
    public void addXp(long userId, int amount, XpSource source, Long refId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        userXpRepository.upsertAddXp(userId, amount); // tự tạo dòng lần đầu
        xpEventRepository.save(XpEvent.builder()
                .user(user)
                .source(source)
                .refId(refId)
                .amount(amount)
                .build());

        int level = levelCatalog.levelFor(userXpRepository.findTotalXp(userId));
        if (userXpRepository.raiseLevel(userId, level) == 1) {
            publisher.publishEvent(new LevelUpEvent(userId, level, LocalDateTime.now()));
        }
    }
}
