package com.elingo.admin.gamification.dto.response;

import com.elingo.gamification.entity.XpSource;

public record AdminXpPolicyResponse(
        XpSource source,
        Integer xp,
        Integer dailyLimit
) {}
