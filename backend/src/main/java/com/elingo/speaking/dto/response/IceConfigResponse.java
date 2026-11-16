package com.elingo.speaking.dto.response;

import java.util.List;

public record IceConfigResponse(
        List<IceServerResponse> iceServers,
        long ttlSeconds
) {
}
