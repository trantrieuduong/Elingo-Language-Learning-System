package com.elingo.speaking.service.impl;

import com.elingo.speaking.config.SpeakingProperties;
import com.elingo.speaking.dto.response.IceConfigResponse;
import com.elingo.speaking.dto.response.IceServerResponse;
import com.elingo.speaking.service.SpeakingIceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpeakingIceServiceImpl implements SpeakingIceService {

    private final SpeakingProperties props;

    @Override
    public IceConfigResponse getIceConfig(Long userId) {
        List<IceServerResponse> servers = List.of(
                new IceServerResponse(props.stunUrls(), null, null));
        return new IceConfigResponse(servers, 0L);
    }
}
