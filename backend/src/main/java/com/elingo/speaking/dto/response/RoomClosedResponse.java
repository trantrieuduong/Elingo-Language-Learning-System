package com.elingo.speaking.dto.response;

public record RoomClosedResponse(
        String roomCode,
        String reason
) {
}
