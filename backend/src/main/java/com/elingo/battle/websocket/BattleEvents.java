package com.elingo.battle.websocket;

public final class BattleEvents {
    private BattleEvents() {}

    public static final String QUEUE_JOINED = "queue:joined";
    public static final String QUEUE_LEFT = "queue:left";
    public static final String QUEUE_TIMEOUT = "queue:timeout";
    public static final String ROOM_CREATED = "room:created";
    public static final String ROOM_LEFT = "room:left";
    public static final String MATCH_FOUND = "match:found";
    public static final String MATCH_QUESTION = "match:question";
    public static final String MATCH_OPPONENT_ANSWERED = "match:opponent-answered";
    public static final String MATCH_ROUND_RESULT = "match:round-result";
    public static final String MATCH_FINISHED = "match:finished";
    public static final String MATCH_CANCELLED = "match:cancelled";
    public static final String MATCH_OPPONENT_DISCONNECTED = "match:opponent-disconnected";
    public static final String MATCH_OPPONENT_RECONNECTED = "match:opponent-reconnected";
    public static final String MATCH_REJOINED = "match:rejoined";
    public static final String ACTIVE_MATCH = "active-match";
    public static final String ERROR = "error";
}
