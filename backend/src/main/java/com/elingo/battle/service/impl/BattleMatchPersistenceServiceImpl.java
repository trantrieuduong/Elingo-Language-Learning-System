package com.elingo.battle.service.impl;

import com.elingo.battle.dto.response.CreatedMatchResult;
import com.elingo.battle.dto.response.PlayerResponse;
import com.elingo.battle.entity.BattleMatch;
import com.elingo.battle.entity.BattleMatchStatus;
import com.elingo.battle.entity.BattleMatchType;
import com.elingo.battle.entity.BattleMode;
import com.elingo.battle.entity.BattleParticipant;
import com.elingo.battle.mapper.BattleMapper;
import com.elingo.battle.repository.BattleMatchRepository;
import com.elingo.battle.repository.BattleParticipantRepository;
import com.elingo.battle.service.BattleMatchPersistenceService;
import com.elingo.battle.config.BattleProperties;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j(topic = "BATTLE-SERVICE")
@Service
@RequiredArgsConstructor
public class BattleMatchPersistenceServiceImpl implements BattleMatchPersistenceService {

    private final BattleMatchRepository matchRepository;
    private final BattleParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final BattleMapper battleMapper;
    private final BattleProperties battleProperties;

    @Override
    @Transactional
    public CreatedMatchResult persistNewRandomMatch(Long userAId, Long userBId, BattleMode mode) {
        User userA = userRepository.findById(userAId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));
        User userB = userRepository.findById(userBId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        BattleMatch match = BattleMatch.builder()
                .mode(mode)
                .matchType(BattleMatchType.RANDOM)
                .status(BattleMatchStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .totalRounds((short) battleProperties.totalRounds())
                .build();
        matchRepository.save(match);

        BattleParticipant partA = BattleParticipant.builder()
                .match(match)
                .user(userA)
                .build();
        BattleParticipant partB = BattleParticipant.builder()
                .match(match)
                .user(userB)
                .build();
        participantRepository.saveAll(List.of(partA, partB));

        log.info("Match persisted matchId={} mode={} userAId={} userBId={}", match.getId(), mode, userAId, userBId);

        PlayerResponse playerA = battleMapper.toPlayerResponse(userA);
        PlayerResponse playerB = battleMapper.toPlayerResponse(userB);

        return new CreatedMatchResult(match.getId(), mode, userAId, userBId, List.of(playerA, playerB));
    }

    @Override
    @Transactional
    public void updateMatchStatus(Long matchId, BattleMatchStatus status) {
        matchRepository.findById(matchId).ifPresent(m -> {
            m.setStatus(status);
            matchRepository.save(m);
        });
    }
}
