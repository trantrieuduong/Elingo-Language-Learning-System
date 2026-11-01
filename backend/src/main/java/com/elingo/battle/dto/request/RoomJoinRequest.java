package com.elingo.battle.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RoomJoinRequest(
        @NotBlank(message = "BATTLE_ROOM_NOT_FOUND") 
        String roomCode
) {
}

