package com.elingo.speaking.service.impl;

import com.elingo.common.dto.PageResponse;
import com.elingo.speaking.dto.request.SpeakingRoomSearchRequest;
import com.elingo.speaking.dto.response.SpeakingHistoryItemResponse;
import com.elingo.speaking.dto.response.SpeakingHistorySummaryResponse;
import com.elingo.speaking.entity.SpeakingParticipant;
import com.elingo.speaking.entity.SpeakingRoom;
import com.elingo.speaking.entity.SpeakingRoomStatus;
import com.elingo.speaking.repository.SpeakingParticipantRepository;
import com.elingo.speaking.service.SpeakingHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j(topic = "SPEAKING-HISTORY-SERVICE")
@Service
@RequiredArgsConstructor
public class SpeakingHistoryServiceImpl implements SpeakingHistoryService {

    private final SpeakingParticipantRepository participantRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SpeakingHistoryItemResponse> getHistory(Long userId, SpeakingRoomSearchRequest request, Pageable pageable) {
        String keyword = request != null ? request.keyword() : null;
        Integer maxP = request != null ? request.maxParticipants() : null;

        Page<SpeakingParticipantRepository.HistoryRoomRow> rows = participantRepository.findHistoryRooms(
                userId,
                likePattern(keyword),
                maxP == null ? 0 : maxP, // 0 = không lọc theo sức chứa
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())
        );

        List<Long> roomIds = rows.getContent().stream()
                .map(SpeakingParticipantRepository.HistoryRoomRow::getRoomId)
                .toList();

        if (roomIds.isEmpty()) {
            return PageResponse.of(new PageImpl<>(List.of(), rows.getPageable(), rows.getTotalElements()));
        }

        Map<Long, List<SpeakingParticipant>> byRoom = participantRepository
                .findFinishedByUserAndRoomIds(userId, roomIds).stream()
                .collect(Collectors.groupingBy(p -> p.getRoom().getId(), LinkedHashMap::new, Collectors.toList()));

        // Mỗi phòng -> một dòng lịch sử
        return PageResponse.of(rows.map(row -> {
            List<SpeakingParticipant> visits = byRoom.get(row.getRoomId());
            SpeakingParticipant first = visits.get(0); // lượt đầu tiên
            SpeakingParticipant last = visits.get(visits.size() - 1); // lượt gần nhất
            SpeakingRoom room = first.getRoom();

            // Tổng thời gian = cộng thời gian từng lượt
            long total = visits.stream()
                    .mapToLong(v -> Math.max(0, Duration.between(v.getJoinedAt(), v.getLeftAt()).getSeconds()))
                    .sum();

            LocalDateTime lastLeft = visits.stream()
                    .map(SpeakingParticipant::getLeftAt)
                    .max(Comparator.naturalOrder())
                    .orElse(last.getLeftAt());

            boolean kicked = visits.stream().anyMatch(v -> v.getKickedAt() != null); // kick đặt cả left_at và kicked_at; unban đặt kicked_at = null
            boolean canRejoin = room.getStatus() != SpeakingRoomStatus.CLOSED && !kicked; // Vào lại được khi phòng chưa đóng và không bị kick

            return new SpeakingHistoryItemResponse(
                    room.getRoomCode(),
                    room.getTopic(),
                    room.getMaxParticipants(),
                    last.getRole().name(),
                    visits.size(),
                    first.getJoinedAt(),
                    lastLeft,
                    total,
                    room.getStatus(),
                    canRejoin
            );
        }));
    }

    @Override
    @Transactional(readOnly = true)
    public SpeakingHistorySummaryResponse getSummary(Long userId) {
        SpeakingParticipantRepository.HistorySummaryRow r = participantRepository.summarize(userId);
        return new SpeakingHistorySummaryResponse(
                nz(r != null ? r.getTotalSeconds() : null),
                nz(r != null ? r.getRoomCount() : null),
                nz(r != null ? r.getSessionCount() : null)
        );
    }

    private static String likePattern(String keyword) {
        if (!StringUtils.hasText(keyword)) return "%";
        String k = keyword.trim().toLowerCase()
                .replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + k + "%";
    }

    // null -> 0 để summary không bị NPE khi user chưa có lượt nào
    private static long nz(Long v) {
        return v == null ? 0 : v;
    }
}
