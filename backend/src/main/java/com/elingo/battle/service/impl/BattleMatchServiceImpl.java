package com.elingo.battle.service.impl;

import com.elingo.battle.config.BattleProperties;
import com.elingo.battle.dto.response.MatchFoundResponse;
import com.elingo.battle.dto.response.CreatedMatchResult;
import com.elingo.battle.entity.BattleMatchType;
import com.elingo.battle.entity.BattleMode;
import com.elingo.battle.service.BattleEngineService;
import com.elingo.battle.service.BattleMatchPersistenceService;
import com.elingo.battle.service.BattleMatchService;
import com.elingo.battle.state.BattleLiveRegistry;
import com.elingo.battle.state.LiveMatch;
import com.elingo.battle.websocket.BattleEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BattleMatchServiceImpl implements BattleMatchService {

    private final BattleMatchPersistenceService persistenceService;
    private final BattleEventPublisher eventPublisher;
    private final BattleProperties battleProperties;
    private final BattleLiveRegistry registry;
    private final BattleEngineService engineService;

    @Override
    public void createRandomMatch(Long userAId, Long userBId, BattleMode mode) {
        // 1. Lưu DB + sinh câu hỏi
        CreatedMatchResult data = persistenceService.persistNewRandomMatch(userAId, userBId, mode);

        // 2. Đưa vào RAM registry
        LiveMatch live = new LiveMatch(data.matchId(), mode, battleProperties.totalRounds(),
                userAId, userBId, data.participantIds(), data.questions());
        registry.register(live);

        // 3. Gửi match:found cho cả hai
        MatchFoundResponse found = new MatchFoundResponse(
                data.matchId(), mode, BattleMatchType.RANDOM,
                battleProperties.totalRounds(), battleProperties.countdownMs(), data.players());
        eventPublisher.sendToUser(userAId, "match:found", found);
        eventPublisher.sendToUser(userBId, "match:found", found);

        // 4. Bắt đầu game loop
        engineService.start(live);
    }

    @Override
    public boolean isUserInActiveMatch(Long userId) {
        return registry.isUserInActiveMatch(userId);
    }
}
