package com.elingo.speaking.mapper;

import com.elingo.speaking.dto.response.SpeakingPeerResponse;
import com.elingo.speaking.entity.ParticipantRole;
import com.elingo.speaking.entity.SpeakingParticipant;
import com.elingo.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SpeakingMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "displayName", source = "user.fullName")
    @Mapping(target = "avatarUrl", source = "user.avatarUrl")
    @Mapping(target = "role", source = "role")
    SpeakingPeerResponse toPeer(SpeakingParticipant participant);

    default SpeakingPeerResponse toHostPeer(User host) {
        return new SpeakingPeerResponse(host.getId(), host.getFullName(), host.getAvatarUrl(), ParticipantRole.HOST);
    }
}
