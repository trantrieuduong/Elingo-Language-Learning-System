package com.elingo.speaking.service;

import com.elingo.speaking.dto.response.IceConfigResponse;

public interface SpeakingIceService {
    IceConfigResponse getIceConfig(Long userId);
}
