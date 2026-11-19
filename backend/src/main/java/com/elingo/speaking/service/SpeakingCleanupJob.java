package com.elingo.speaking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j(topic = "SPEAKING-CLEANUP-JOB")
@Component
@RequiredArgsConstructor
public class SpeakingCleanupJob {

    private final SpeakingCleanupService cleanupService;

    @Scheduled(fixedDelayString = "${app.speaking.cleanup-interval-ms:60000}",
               initialDelayString = "${app.speaking.cleanup-interval-ms:60000}")
    public void run() {
        try {
            int n = cleanupService.cleanStaleRooms();
            if (n > 0) log.info("stale rooms closed: {}", n);
        } catch (Exception ex) { // đảm bảo job không bị dừng khi có lỗi
            log.error("speaking cleanup failed", ex);
        }
    }
}
