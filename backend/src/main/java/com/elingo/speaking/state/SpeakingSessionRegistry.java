package com.elingo.speaking.state;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Quản lý các session STOMP đang subscribe kênh /user/queue/speaking theo userId
 * Hỗ trợ nhiều tab và xử lý mất kết nối
 */
@Component
public class SpeakingSessionRegistry {

    private final Map<Long, Set<String>> sessions = new HashMap<>();
    private final Map<String, Long> speakingSubscriptions = new HashMap<>();

    public synchronized void addSubscription(Long userId, String sessionId, String subscriptionId) {
        sessions.computeIfAbsent(userId, k -> new HashSet<>()).add(sessionId);
        speakingSubscriptions.put(makeKey(sessionId, subscriptionId), userId);
    }

    // Gỡ subscription speaking và trả về userId nếu không còn session nào đang subscribe
    public synchronized Long removeSubscription(String sessionId, String subscriptionId) {
        String key = makeKey(sessionId, subscriptionId);
        Long userId = speakingSubscriptions.remove(key);
        if (userId == null) return null;

        // Kiểm tra session còn subscription speaking nào không
        boolean sessionStillHasSpeakingSub = false;
        String sessionPrefix = sessionId + ":";
        for (String k : speakingSubscriptions.keySet()) {
            if (k.startsWith(sessionPrefix)) {
                sessionStillHasSpeakingSub = true;
                break;
            }
        }

        if (!sessionStillHasSpeakingSub) {
            Set<String> set = sessions.get(userId);
            if (set != null) {
                set.remove(sessionId);
                if (set.isEmpty()) {
                    sessions.remove(userId);
                    return userId;
                }
            }
        }
        return null;
    }

    // Xử lý session khi user ngắt kết nối
    public synchronized boolean removeAndCheckLast(Long userId, String sessionId) {
        String sessionPrefix = sessionId + ":";
        Iterator<Map.Entry<String, Long>> it = speakingSubscriptions.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getKey().startsWith(sessionPrefix)) it.remove();
        }

        Set<String> set = sessions.get(userId);
        if (set == null) return true;

        boolean removed = set.remove(sessionId);
        if (!set.isEmpty()) return false;

        sessions.remove(userId);
        return removed;
    }

    private String makeKey(String sessionId, String subscriptionId) {
        return sessionId + ":" + (subscriptionId != null ? subscriptionId : "default");
    }
}
