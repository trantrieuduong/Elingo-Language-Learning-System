package com.elingo.speaking.state;

import com.elingo.speaking.entity.ParticipantRole;
import lombok.Getter;
import lombok.Setter;

/**
 * Lưu trạng thái và thông tin thành viên đang hoạt động trong phòng speaking
 */
@Getter
public class LivePeer {
    private final Long userId;
    private final String displayName;
    private final String avatarUrl;
    private volatile ParticipantRole role;
    @Setter private volatile boolean online = true;
    @Setter private volatile boolean micMuted; // mặc định false; chỉ RAM, không lưu DB

    public LivePeer(Long userId, String displayName, String avatarUrl, ParticipantRole role) {
        this.userId = userId;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.role = role;
    }

    public void setRole(ParticipantRole role) {
        this.role = role;
    }
}
