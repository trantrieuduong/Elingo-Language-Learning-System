package com.elingo.battle.service;

import com.elingo.battle.entity.BattleMode;

public interface BattleRoomService {
    void createRoom(Long userId, BattleMode mode);
    void joinRoom(Long userId, String roomCode);
    void leaveRoom(Long userId);
    void onDisconnect(Long userId);
}

