package com.elingo.speaking.websocket;

public final class SpeakingEvents {
    private SpeakingEvents() {}

    public static final String ROOM_STATE = "room:state";
    public static final String ROOM_CREATED = "room:created";
    public static final String ROOM_LEFT = "room:left";
    public static final String PEER_JOINED = "peer:joined";
    public static final String PEER_LEFT = "peer:left";
    public static final String PEER_DISCONNECTED = "peer:disconnected"; 
    public static final String PEER_RECONNECTED = "peer:reconnected";   
    public static final String HOST_CHANGED = "host:changed";         
    public static final String ROOM_CLOSED = "room:closed";         
    public static final String SIGNAL = "signal";
    public static final String PEER_MIC = "peer:mic";
    public static final String MUTE_REQUESTED = "mute:requested";
    public static final String ROOM_KICKED = "room:kicked";
    public static final String BAN_REMOVED = "ban:removed";
    public static final String CHAT_MESSAGE = "chat:message";
    public static final String ROOM_ACTIVE = "room:active";
    public static final String ERROR = "error";
}
