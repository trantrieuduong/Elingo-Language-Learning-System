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
        CreatedMatchResult data = persistenceService.persistNewRandomMatch(userAId, userBId, mode);
        launch(data, BattleMatchType.RANDOM);
    }

    @Override
    public void createRoomMatch(Long hostId, Long guestId, BattleMode mode, String roomCode) {
        CreatedMatchResult data = persistenceService.persistNewRoomMatch(hostId, guestId, mode, roomCode);
        launch(data, BattleMatchType.ROOM);
    }

    private void launch(CreatedMatchResult data, BattleMatchType type) {
        // Đưa vào RAM registry
        LiveMatch live = new LiveMatch(data.matchId(), data.mode(), type, battleProperties.totalRounds(),
                data.userAId(), data.userBId(), data.participantIds(), data.questions());
        live.setPlayers(data.players());
        registry.register(live);

        // Gửi match:found cho cả hai
        MatchFoundResponse found = new MatchFoundResponse(
                data.matchId(), data.mode(), type,
                battleProperties.totalRounds(), battleProperties.countdownMs(), data.players());
        eventPublisher.sendToUser(data.userAId(), "match:found", found);
        eventPublisher.sendToUser(data.userBId(), "match:found", found);

        // Bắt đầu game loop 
        engineService.start(live);
    }

    @Override
    // Kiểm tra user đang trong trận đấu
    public boolean isUserInActiveMatch(Long userId) {
        return registry.isUserInActiveMatch(userId);
    }
}
