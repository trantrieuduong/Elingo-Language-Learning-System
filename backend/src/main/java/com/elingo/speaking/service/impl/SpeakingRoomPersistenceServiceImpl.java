package com.elingo.speaking.service.impl;

import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.speaking.config.SpeakingProperties;
import com.elingo.speaking.dto.request.SpeakingRoomSearchRequest;
import com.elingo.speaking.dto.response.SpeakingBannedUserResponse;
import com.elingo.speaking.dto.response.SpeakingPeerResponse;
import com.elingo.speaking.dto.response.SpeakingRoomDetailResponse;
import com.elingo.speaking.dto.response.SpeakingRoomResponse;
import com.elingo.speaking.entity.ParticipantRole;
import com.elingo.speaking.entity.SpeakingParticipant;
import com.elingo.speaking.entity.SpeakingRoom;
import com.elingo.speaking.entity.SpeakingRoomStatus;
import com.elingo.speaking.mapper.SpeakingMapper;
import com.elingo.speaking.repository.SpeakingParticipantRepository;
import com.elingo.speaking.repository.SpeakingParticipantRepository.RoomCountProjection;
import com.elingo.speaking.repository.SpeakingRoomRepository;
import com.elingo.speaking.repository.specification.SpeakingRoomFilter;
import com.elingo.speaking.repository.specification.SpeakingRoomSpecifications;
import com.elingo.speaking.service.SpeakingRoomPersistenceService;
import com.elingo.speaking.util.SpeakingRoomCode;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j(topic = "SPEAKING-ROOM-PERSISTENCE-SERVICE")
@Service
@RequiredArgsConstructor
public class SpeakingRoomPersistenceServiceImpl implements SpeakingRoomPersistenceService {

    private final SpeakingRoomRepository roomRepository;
    private final SpeakingParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final SpeakingMapper mapper;
    private final SpeakingProperties props;

    @Override
    @Transactional
    public SpeakingRoomDetailResponse createRoom(Long userId, String topic, Integer maxParticipants, Boolean isPrivate) {
        // Kiểm tra user chưa tham gia phòng nào
        if (participantRepository.existsByUserIdAndLeftAtIsNull(userId)) {
            throw new AppException(AppError.SPEAKING_ALREADY_IN_ROOM);
        }

        User host = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));
        int max = maxParticipants == null ? props.defaultMaxParticipants()
                : Math.min(maxParticipants, props.maxParticipantsLimit());

        String code = null;
        for (int i = 0; i < 10; i++) {
            String candidate = SpeakingRoomCode.generate();
            if (!roomRepository.existsByRoomCode(candidate)) {
                code = candidate;
                break;
            }
        }

        if (code == null) {
            log.error("Failed to generate unique speaking room code after 10 attempts");
            throw new AppException(AppError.UNCATEGORIZED_EXCEPTION);
        }

        SpeakingRoom room = roomRepository.save(SpeakingRoom.builder()
                .roomCode(code)
                .host(host)
                .topic(topic == null || topic.isBlank() ? null : topic.trim())
                .maxParticipants((short) max)
                .isPrivate(Boolean.TRUE.equals(isPrivate))
                .status(SpeakingRoomStatus.WAITING)
                .build());
        participantRepository.save(SpeakingParticipant.builder()
                .room(room).user(host).role(ParticipantRole.HOST).build());

        log.info("Speaking room created roomCode={} hostId={} max={}", code, userId, max);
        return buildDetail(room);
    }

    @Override
    @Transactional
    public GoOnlineContext joinRoom(Long userId, String rawCode) {
        String code = SpeakingRoomCode.normalize(rawCode);
        SpeakingRoom room = roomRepository.findByRoomCodeForUpdate(code)
                .orElseThrow(() -> new AppException(AppError.SPEAKING_ROOM_NOT_FOUND));
        if (room.getStatus() == SpeakingRoomStatus.CLOSED) {
            throw new AppException(AppError.SPEAKING_ROOM_CLOSED);
        }

        if (participantRepository.existsByRoomIdAndUserIdAndKickedAtIsNotNull(room.getId(), userId)) {
            throw new AppException(AppError.SPEAKING_BANNED);
        }

        // Đã ở chính phòng này -> idempotent (F5, mở lại trang)
        if (participantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(room.getId(), userId).isPresent()) {
            return buildJoinContext(room, userId);
        }

        // Đảm bảo mỗi user chỉ tham gia một phòng tại một thời điểm
        if (participantRepository.existsByUserIdAndLeftAtIsNull(userId)) {
            throw new AppException(AppError.SPEAKING_ALREADY_IN_ROOM);
        }

        long current = participantRepository.countByRoomIdAndLeftAtIsNull(room.getId());
        if (current >= room.getMaxParticipants()) {
            throw new AppException(AppError.SPEAKING_ROOM_FULL);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));
        participantRepository.save(SpeakingParticipant.builder()
                .room(room).user(user).role(ParticipantRole.GUEST).build());

        if (room.getStatus() == SpeakingRoomStatus.WAITING) {
            room.setStatus(SpeakingRoomStatus.ACTIVE);
            room.setStartedAt(LocalDateTime.now());
        }
        log.info("Speaking room joined roomCode={} userId={}", code, userId);
        return buildJoinContext(room, userId);
    }

    @Override
    @Transactional
    public LeaveResult leaveRoom(Long userId, String rawCode) {
        String code = SpeakingRoomCode.normalize(rawCode);
        SpeakingRoom room = roomRepository.findByRoomCodeForUpdate(code)
                .orElseThrow(() -> new AppException(AppError.SPEAKING_ROOM_NOT_FOUND));
        SpeakingParticipant me = participantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(room.getId(), userId)
                .orElseThrow(() -> new AppException(AppError.SPEAKING_PARTICIPANT_NOT_FOUND));

        // Đánh dấu rời phòng và tính thời gian tham gia
        LocalDateTime now = LocalDateTime.now();
        me.setLeftAt(now);
        long duration = Math.max(0, Duration.between(me.getJoinedAt(), now).getSeconds());

        List<SpeakingParticipant> remaining = participantRepository.findActiveByRoomId(room.getId())
                .stream().filter(p -> !p.getId().equals(me.getId())).toList();

        Long newHostId = null;
        boolean closed = false;

        // Đóng phòng nếu không còn thành viên, hoặc chuyển HOST nếu chủ phòng rời đi
        if (remaining.isEmpty()) {
            room.setStatus(SpeakingRoomStatus.CLOSED);
            room.setEndedAt(now);
            closed = true;
        } else if (room.getHost().getId().equals(userId)) {
            SpeakingParticipant next = remaining.get(0);
            next.setRole(ParticipantRole.HOST);
            room.setHost(next.getUser());
            newHostId = next.getUser().getId();
        }
        log.info("Speaking room left roomCode={} userId={} newHostId={} closed={}", code, userId, newHostId, closed);
        return new LeaveResult(code, room.getTopic(), duration, newHostId, closed);
    }

    @Override
    @Transactional
    public SpeakingRoomDetailResponse transferHost(Long userId, String rawCode, Long newHostId) {
        SpeakingRoom room = lockOpenRoom(rawCode);
        requireHost(room, userId); // SPEAKING_NOT_HOST nếu không phải host
        if (newHostId.equals(userId)) {
            throw new AppException(AppError.INVALID_REQUEST);
        }
        SpeakingParticipant target = participantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(room.getId(), newHostId)
                .orElseThrow(() -> new AppException(AppError.SPEAKING_PARTICIPANT_NOT_FOUND));
        SpeakingParticipant me = participantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(room.getId(), userId)
                .orElseThrow(() -> new AppException(AppError.SPEAKING_PARTICIPANT_NOT_FOUND));
        me.setRole(ParticipantRole.GUEST);
        target.setRole(ParticipantRole.HOST);
        room.setHost(target.getUser());
        return buildDetail(room);
    }

    @Override
    @Transactional
    public EndResult endRoom(Long userId, String rawCode) {
        SpeakingRoom room = lockOpenRoom(rawCode);
        requireHost(room, userId);
        LocalDateTime now = LocalDateTime.now();
        SpeakingParticipant me = participantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(room.getId(), userId)
                .orElseThrow(() -> new AppException(AppError.SPEAKING_PARTICIPANT_NOT_FOUND));
        long duration = Math.max(0, Duration.between(me.getJoinedAt(), now).getSeconds());
        participantRepository.closeOpenParticipants(room.getId(), now);
        room.setStatus(SpeakingRoomStatus.CLOSED);
        room.setEndedAt(now);
        return new EndResult(room.getRoomCode(), room.getTopic(), duration);
    }

    @Override
    @Transactional
    public SpeakingRoomDetailResponse kickMember(Long hostId, String rawCode, Long targetUserId) {
        SpeakingRoom room = lockOpenRoom(rawCode);
        requireHost(room, hostId);
        if (targetUserId.equals(hostId)) throw new AppException(AppError.INVALID_REQUEST); // không tự kick mình
        SpeakingParticipant p = participantRepository.findByRoomIdAndUserIdAndLeftAtIsNull(room.getId(), targetUserId)
                .orElseThrow(() -> new AppException(AppError.SPEAKING_PARTICIPANT_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now();
        p.setLeftAt(now); // đánh dấu rời phòng
        p.setKickedAt(now); // đánh dấu bị chặn
        log.info("Speaking member kicked roomCode={} hostId={} targetId={}", room.getRoomCode(), hostId, targetUserId);
        return buildDetail(room);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpeakingBannedUserResponse> listBans(Long hostId, String rawCode) {
        SpeakingRoom room = roomRepository.findByRoomCodeWithHost(SpeakingRoomCode.normalize(rawCode))
                .orElseThrow(() -> new AppException(AppError.SPEAKING_ROOM_NOT_FOUND));
        if (room.getStatus() == SpeakingRoomStatus.CLOSED) {
            throw new AppException(AppError.SPEAKING_ROOM_CLOSED);
        }
        requireHost(room, hostId);
        return toBannedList(room.getId());
    }

    @Override
    @Transactional
    public List<SpeakingBannedUserResponse> unbanMember(Long hostId, String rawCode, Long targetUserId) {
        SpeakingRoom room = lockOpenRoom(rawCode);
        requireHost(room, hostId);
        SpeakingParticipant p = participantRepository.findFirstByRoomIdAndUserIdAndKickedAtIsNotNull(room.getId(), targetUserId)
                .orElseThrow(() -> new AppException(AppError.SPEAKING_PARTICIPANT_NOT_FOUND));
        p.setKickedAt(null);                                                                       // từ giờ joinRoom không chặn nữa
        return toBannedList(room.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findStaleRoomCodes(LocalDateTime before) {
        return roomRepository.findOpenRoomCodesCreatedBefore(before);
    }

    @Override
    @Transactional
    public boolean forceCloseRoom(String roomCode) {
        return closeLocked(roomCode, LocalDateTime.now());
    }

    @Override
    @Transactional
    public int closeAllOpenRooms() {
        LocalDateTime now = LocalDateTime.now();
        int n = 0;
        for (String code : roomRepository.findAllOpenRoomCodes()) {
            if (closeLocked(code, now)) {
                n++;
            }
        }
        return n;
    }

    private boolean closeLocked(String roomCode, LocalDateTime now) {
        SpeakingRoom room = roomRepository.findByRoomCodeForUpdate(roomCode).orElse(null);
        if (room == null || room.getStatus() == SpeakingRoomStatus.CLOSED) {
            return false;
        }
        participantRepository.closeOpenParticipants(room.getId(), now);
        room.setStatus(SpeakingRoomStatus.CLOSED);
        room.setEndedAt(now);
        return true;
    }

    private List<SpeakingBannedUserResponse> toBannedList(Long roomId) {
        return participantRepository.findKickedByRoomId(roomId).stream()
                .map(p -> new SpeakingBannedUserResponse(p.getUser().getId(), p.getUser().getFullName(),
                        p.getUser().getAvatarUrl(), p.getKickedAt()))
                .toList();
    }

    private SpeakingRoom lockOpenRoom(String rawCode) {
        SpeakingRoom room = roomRepository.findByRoomCodeForUpdate(SpeakingRoomCode.normalize(rawCode))
                .orElseThrow(() -> new AppException(AppError.SPEAKING_ROOM_NOT_FOUND));
        if (room.getStatus() == SpeakingRoomStatus.CLOSED) {
            throw new AppException(AppError.SPEAKING_ROOM_CLOSED);
        }
        return room;
    }

    private void requireHost(SpeakingRoom room, Long userId) {
        if (!room.getHost().getId().equals(userId)) {
            throw new AppException(AppError.SPEAKING_NOT_HOST);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String findActiveRoomCode(Long userId) {
        return participantRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new AppException(AppError.SPEAKING_PARTICIPANT_NOT_FOUND))
                .getRoom().getRoomCode();
    }

    @Override
    @Transactional(readOnly = true)
    public String findActiveRoomCodeOrNull(Long userId) {
        return participantRepository.findActiveByUserId(userId)
                .map(p -> p.getRoom().getRoomCode())
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public SpeakingRoomDetailResponse getRoomDetail(String rawCode) {
        String code = SpeakingRoomCode.normalize(rawCode);
        SpeakingRoom room = roomRepository.findByRoomCodeWithHost(code)
                .orElseThrow(() -> new AppException(AppError.SPEAKING_ROOM_NOT_FOUND));
        if (room.getStatus() == SpeakingRoomStatus.CLOSED) {
            throw new AppException(AppError.SPEAKING_ROOM_CLOSED);
        }
        return buildDetail(room);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SpeakingRoomResponse> getOpenRooms(SpeakingRoomSearchRequest request, Pageable pageable) {
        String keyword = (request != null && request.keyword() != null && !request.keyword().isBlank())
                ? request.keyword().trim() : null;
        Integer maxParticipants = request != null ? request.maxParticipants() : null;

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt", "id")
        );

        Page<SpeakingRoom> page = roomRepository.findAll(
                SpeakingRoomSpecifications.openLobby(new SpeakingRoomFilter(keyword, maxParticipants)),
                sortedPageable
        );

        List<Long> ids = page.getContent().stream().map(SpeakingRoom::getId).toList();
        Map<Long, Long> counts = ids.isEmpty() ? Map.of()
                : participantRepository.countActiveByRoomIds(ids).stream()
                        .collect(Collectors.toMap(RoomCountProjection::getRoomId, RoomCountProjection::getTotal));

        return PageResponse.of(page.map(r -> new SpeakingRoomResponse(
                r.getRoomCode(), r.getTopic(), r.getMaxParticipants(), r.getStatus(),
                Boolean.TRUE.equals(r.getIsPrivate()),
                counts.getOrDefault(r.getId(), 0L),
                mapper.toHostPeer(r.getHost()),
                r.getStartedAt(), r.getCreatedAt())));
    }

    // Tổng hợp thông tin phòng cùng danh sách thành viên đang hoạt động
    private SpeakingRoomDetailResponse buildDetail(SpeakingRoom room) {
        List<SpeakingPeerResponse> peers = participantRepository.findActiveByRoomId(room.getId())
                .stream().map(mapper::toPeer).toList();
        return new SpeakingRoomDetailResponse(
                room.getRoomCode(), room.getTopic(), room.getMaxParticipants(), room.getStatus(),
                Boolean.TRUE.equals(room.getIsPrivate()),
                room.getHost().getId(), room.getStartedAt(), room.getCreatedAt(), peers);
    }

    // Xây context tham gia phòng từ trạng thái DB hiện tại 
    private GoOnlineContext buildJoinContext(SpeakingRoom room, Long userId) {
        List<SpeakingParticipant> members = participantRepository.findActiveByRoomId(room.getId());
        SpeakingParticipant mePart = members.stream()
                .filter(p -> p.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new AppException(AppError.SPEAKING_ROOM_ACCESS_DENIED));

        Long startedAtMs = room.getStartedAt() == null ? null
                : room.getStartedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        SpeakingPeerResponse me = mapper.toPeer(mePart);
        List<SpeakingPeerResponse> memberResponses = members.stream().map(mapper::toPeer).toList();

        return new GoOnlineContext(
                room.getId(), room.getRoomCode(), room.getTopic(), room.getHost().getId(),
                room.getMaxParticipants(), Boolean.TRUE.equals(room.getIsPrivate()),
                startedAtMs, me, memberResponses);
    }
}
