package com.elingo.gamification.service;

import com.elingo.common.service.RedisService;
import com.elingo.gamification.service.impl.CheckInServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CheckInServiceTest {

    @Mock
    private RedisService redis;

    @Mock
    private CheckInRecorder recorder;

    @InjectMocks
    private CheckInServiceImpl checkInService;

    private static final Long USER_ID = 42L;
    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 14, 8, 0);

    @Nested
    @DisplayName("ensureCheckedIn")
    class EnsureCheckedInTests {

        @Test
        @DisplayName("Check in successfully when this request sets the bit")
        void ensureCheckedIn_Success_BitSetByMe() {
            when(redis.getBit(anyString(), eq(USER_ID))).thenReturn(false);
            when(redis.setBit(anyString(), eq(USER_ID), eq(true))).thenReturn(false);

            checkInService.ensureCheckedIn(USER_ID, AT);

            verify(redis).getBit(anyString(), eq(USER_ID));
            verify(redis).setBit(anyString(), eq(USER_ID), eq(true));
            verify(redis).expireAt(anyString(), any());
            verify(recorder).record(USER_ID, AT.toLocalDate());
        }

        @Test
        @DisplayName("Skip check in when bit already set from previous check in")
        void ensureCheckedIn_Skip_BitAlreadySet() {
            when(redis.getBit(anyString(), eq(USER_ID))).thenReturn(true);

            checkInService.ensureCheckedIn(USER_ID, AT);

            verify(redis).getBit(anyString(), eq(USER_ID));
            verify(redis, never()).setBit(anyString(), anyLong(), anyBoolean());
            verify(redis, never()).expireAt(anyString(), any());
            verify(recorder, never()).record(anyLong(), any());
        }

        @Test
        @DisplayName("Skip check in when another request sets the bit first")
        void ensureCheckedIn_Skip_SetBitReturnsTrue() {
            when(redis.getBit(anyString(), eq(USER_ID))).thenReturn(false);
            when(redis.setBit(anyString(), eq(USER_ID), eq(true))).thenReturn(true);

            checkInService.ensureCheckedIn(USER_ID, AT);

            verify(redis).getBit(anyString(), eq(USER_ID));
            verify(redis).setBit(anyString(), eq(USER_ID), eq(true));
            verify(redis, never()).expireAt(anyString(), any());
            verify(recorder, never()).record(anyLong(), any());
        }

        @Test
        @DisplayName("Fall back to database when redis is unavailable")
        void ensureCheckedIn_Fallback_RedisUnavailable() {
            when(redis.getBit(anyString(), eq(USER_ID))).thenThrow(new RuntimeException("Connection refused"));

            checkInService.ensureCheckedIn(USER_ID, AT);

            verify(redis).getBit(anyString(), eq(USER_ID));
            verify(redis, never()).expireAt(anyString(), any());
            verify(recorder).record(USER_ID, AT.toLocalDate());
        }

        @Test
        @DisplayName("Clear bit and rethrow when database recording fails")
        void ensureCheckedIn_ClearBit_WhenRecorderThrows() {
            when(redis.getBit(anyString(), eq(USER_ID))).thenReturn(false);
            when(redis.setBit(anyString(), eq(USER_ID), eq(true))).thenReturn(false);
            doThrow(new RuntimeException("DB error")).when(recorder).record(anyLong(), any());

            assertThatThrownBy(() -> checkInService.ensureCheckedIn(USER_ID, AT))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB error");

            verify(redis).setBit(anyString(), eq(USER_ID), eq(true));
            verify(recorder).record(USER_ID, AT.toLocalDate());
            verify(redis).setBit(anyString(), eq(USER_ID), eq(false));
        }
    }
}
