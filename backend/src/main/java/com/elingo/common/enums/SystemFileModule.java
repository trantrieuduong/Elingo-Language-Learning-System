package com.elingo.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum SystemFileModule {
    DECKS("decks", Set.of(MediaKind.IMAGE)),
    CARDS("cards", Set.of(MediaKind.IMAGE)),
    PHONETICS("phonetics", Set.of(MediaKind.AUDIO)),
    BADGES("badges", Set.of(MediaKind.IMAGE)),
    LESSONS("lessons", Set.of(MediaKind.IMAGE)),
    MISC("misc", Set.of(MediaKind.IMAGE, MediaKind.AUDIO, MediaKind.VIDEO));

    private final String path;
    private final Set<MediaKind> allowedMediaKinds;

    public boolean supports(MediaKind kind) {
        return allowedMediaKinds.contains(kind);
    }
}
