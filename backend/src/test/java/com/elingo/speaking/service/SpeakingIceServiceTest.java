package com.elingo.speaking.service;

import com.elingo.speaking.config.SpeakingProperties;
import com.elingo.speaking.dto.response.IceConfigResponse;
import com.elingo.speaking.service.impl.SpeakingIceServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class SpeakingIceServiceTest {

    private static final Long USER_ID = 1L;

    // SpeakingProperties là record nên dựng thật và truyền thẳng vào constructor
    private static SpeakingProperties propsWithStun(List<String> stunUrls) {
        return new SpeakingProperties(4, 4, 16384, stunUrls, 15000L, 30000L, 600000L, 60000L);
    }

    // ========================================================================
    // getIceConfig
    // ========================================================================
    @Nested
    @DisplayName("getIceConfig")
    class GetIceConfigTests {

        @Test
        @DisplayName("Get ICE config returns the configured STUN servers without credentials")
        void getIceConfig_Success() {
            List<String> stunUrls = List.of("stun:stun.l.google.com:19302", "stun:stun1.l.google.com:19302");
            SpeakingIceService service = new SpeakingIceServiceImpl(propsWithStun(stunUrls));

            IceConfigResponse response = service.getIceConfig(USER_ID);

            assertThat(response.iceServers()).hasSize(1);
            assertThat(response.iceServers().get(0).urls()).containsExactlyElementsOf(stunUrls);
            assertThat(response.iceServers().get(0).username()).isNull();
            assertThat(response.iceServers().get(0).credential()).isNull();
        }

        @Test
        @DisplayName("Get ICE config returns a time-to-live of zero because STUN needs no credentials")
        void getIceConfig_Success_NoTtl() {
            SpeakingIceService service = new SpeakingIceServiceImpl(propsWithStun(List.of("stun:example.org:3478")));

            assertThat(service.getIceConfig(USER_ID).ttlSeconds()).isZero();
        }

        @Test
        @DisplayName("Get ICE config returns the same servers for every user")
        void getIceConfig_Success_SameForEveryUser() {
            SpeakingIceService service = new SpeakingIceServiceImpl(propsWithStun(List.of("stun:example.org:3478")));

            assertThat(service.getIceConfig(1L)).isEqualTo(service.getIceConfig(2L));
        }

        @Test
        @DisplayName("Get ICE config returns an ICE server with no URLs when none are configured")
        void getIceConfig_Success_NoStunUrls() {
            SpeakingIceService service = new SpeakingIceServiceImpl(propsWithStun(List.of()));

            IceConfigResponse response = service.getIceConfig(USER_ID);

            assertThat(response.iceServers()).hasSize(1);
            assertThat(response.iceServers().get(0).urls()).isEmpty();
        }
    }
}
