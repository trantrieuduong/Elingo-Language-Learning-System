package com.elingo.speaking.repository.specification;

public record SpeakingRoomFilter(
        String keyword,
        Integer maxParticipants
) {
}
