package com.elingo.gamification.service;

import java.time.LocalDateTime;

public interface CheckInService {
    void ensureCheckedIn(long userId, LocalDateTime at);
}
