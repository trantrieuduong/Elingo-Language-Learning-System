package com.elingo.battle.state;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Quản lý các session STOMP đang subscribe kênh Battle theo userId.
 * Hỗ trợ nhiều tab và xử lý mất kết nối.
 */
@Component
public class BattleSessionRegistry {

    private final Map<Long, Set<String>> sessions = new HashMap<>();

    private final Map<String, Long> battleSubscriptions = new HashMap<>();

    public synchronized void addSubscription(Long userId, String sessionId, String subscriptionId) {
        sessions.computeIfAbsent(userId, k -> new HashSet<>()).add(sessionId);
        battleSubscriptions.put(makeKey(sessionId, subscriptionId), userId);
    }

    // Gỡ subscription battle và trả về userId nếu không còn session nào
    public synchronized Long removeSubscription(String sessionId, String subscriptionId) {
        String key = makeKey(sessionId, subscriptionId);
        Long userId = battleSubscriptions.remove(key);
        if (userId == null) return null;

        // Kiểm tra session còn subscription battle nào không
        boolean sessionStillHasBattleSub = false;
        String sessionPrefix = sessionId + ":";
        for (String k : battleSubscriptions.keySet()) {
            if (k.startsWith(sessionPrefix)) {
                sessionStillHasBattleSub = true;
                break;
            }
        }

        // Xóa session khỏi danh sách của user nếu không còn
        if (!sessionStillHasBattleSub) {
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
        // Xóa các key phụ của session bị disconnect
        String sessionPrefix = sessionId + ":";
        Iterator<Map.Entry<String, Long>> it = battleSubscriptions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (entry.getKey().startsWith(sessionPrefix)) {
                it.remove();
            }
        }

        Set<String> set = sessions.get(userId);
        if (set == null)
            return true;

        boolean removed = set.remove(sessionId);
        if (!set.isEmpty())
            return false;

        sessions.remove(userId);
        return removed;
    }
    
    // Tạo key phân biệt các subscription trong cùng session
    private String makeKey(String sessionId, String subscriptionId) {
        return sessionId + ":" + (subscriptionId != null ? subscriptionId : "default");
    }
}
