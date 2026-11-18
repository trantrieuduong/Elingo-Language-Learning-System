package com.elingo.speaking.service;

public interface SpeakingCleanupService {
    int cleanStaleRooms();
    int cleanAllOnStartup();
}
