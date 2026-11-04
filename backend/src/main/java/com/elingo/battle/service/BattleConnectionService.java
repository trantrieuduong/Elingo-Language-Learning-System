package com.elingo.battle.service;

public interface BattleConnectionService {
    void onUserOffline(Long userId);
    void rejoin(Long userId, Long matchId);
    void sendActiveMatch(Long userId);
}

