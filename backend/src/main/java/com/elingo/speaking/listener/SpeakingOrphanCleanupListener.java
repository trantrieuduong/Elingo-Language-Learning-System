package com.elingo.speaking.listener;

import com.elingo.speaking.service.SpeakingCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Dọn các phòng Speaking còn sót lại từ lần chạy trước
@Slf4j(topic = "SPEAKING-ORPHAN-CLEANUP")
@Component
@RequiredArgsConstructor
public class SpeakingOrphanCleanupListener {

    private final SpeakingCleanupService cleanupService;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            cleanupService.cleanAllOnStartup();
        } catch (Exception ex) {
            log.error("speaking orphan cleanup failed", ex);
        }
    }
}
