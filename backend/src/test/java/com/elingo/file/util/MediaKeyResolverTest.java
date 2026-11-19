package com.elingo.file.util;

import com.elingo.common.enums.MediaKind;
import com.elingo.common.enums.SystemFileModule;
import com.elingo.common.event.FileAttachedEvent;
import com.elingo.common.event.FileDeletedEvent;
import com.elingo.common.event.SystemFileAttachedEvent;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class MediaKeyResolverTest {

    static class RecordingEventPublisher implements ApplicationEventPublisher {
        final List<Object> events = new ArrayList<>();

        @Override
        public void publishEvent(Object event) {
            events.add(event);
        }
    }

    private RecordingEventPublisher publisher;
    private MediaKeyResolver resolver;

    @BeforeEach
    void setUp() {
        publisher = new RecordingEventPublisher();
        resolver = new MediaKeyResolver(publisher);
    }

    // ========================================================================
    // 1. forCreate(verifiedKey, MediaKind) — User Create
    // ========================================================================
    @Nested
    @DisplayName("forCreate with MediaKind (User)")
    class ForCreateUserTests {

        @Test
        @DisplayName("valid verified image key -> uploads/{userId}/{filename}")
        void validImageSuccess() {
            var res = resolver.forCreate("verified/12/avatar.png", MediaKind.IMAGE);
            assertThat(res.key()).isEqualTo("uploads/12/avatar.png");
            assertThat(res.verifiedKey()).isEqualTo("verified/12/avatar.png");
            assertThat(res.obsoleteKeys()).isEmpty();
        }

        @Test
        @DisplayName("valid verified audio key -> uploads/{userId}/{filename}")
        void validAudioSuccess() {
            var res = resolver.forCreate("verified/12/voice.mp3", MediaKind.AUDIO);
            assertThat(res.key()).isEqualTo("uploads/12/voice.mp3");
            assertThat(res.verifiedKey()).isEqualTo("verified/12/voice.mp3");
            assertThat(res.obsoleteKeys()).isEmpty();
        }

        @Test
        @DisplayName("null or blank verifiedKey -> EMPTY")
        void nullOrBlank() {
            assertThat(resolver.forCreate(null, MediaKind.IMAGE).key()).isNull();
            assertThat(resolver.forCreate("   ", MediaKind.IMAGE).key()).isNull();
        }

        @Test
        @DisplayName("key not in verified zone -> INVALID_REQUEST")
        void keyNotInVerifiedZone() {
            assertThatThrownBy(() -> resolver.forCreate("staging/12/avatar.png", MediaKind.IMAGE))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_REQUEST));

            assertThatThrownBy(() -> resolver.forCreate("uploads/12/avatar.png", MediaKind.IMAGE))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_REQUEST));
        }

        @Test
        @DisplayName("media kind mismatch (audio for image) -> INVALID_FILE_TYPE")
        void mediaKindMismatch() {
            assertThatThrownBy(() -> resolver.forCreate("verified/12/voice.mp3", MediaKind.IMAGE))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_FILE_TYPE));
        }

        @Test
        @DisplayName("unsupported extension or no extension -> INVALID_FILE_TYPE")
        void unknownExtension() {
            assertThatThrownBy(() -> resolver.forCreate("verified/12/file.exe", MediaKind.IMAGE))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_FILE_TYPE));

            assertThatThrownBy(() -> resolver.forCreate("verified/12/noext", MediaKind.IMAGE))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_FILE_TYPE));
        }
    }

    // ========================================================================
    // 2. forCreate(verifiedKey, SystemFileModule) — Admin System Create
    // ========================================================================
    @Nested
    @DisplayName("forCreate with SystemFileModule (Admin)")
    class ForCreateSystemTests {

        @Test
        @DisplayName("valid verified key for DECKS -> system/decks/{filename}")
        void validDeckSuccess() {
            var res = resolver.forCreate("verified/12/banner.png", SystemFileModule.DECKS);
            assertThat(res.key()).isEqualTo("system/decks/banner.png");
            assertThat(res.verifiedKey()).isEqualTo("verified/12/banner.png");
            assertThat(res.obsoleteKeys()).isEmpty();
        }

        @Test
        @DisplayName("valid verified key for CARDS -> system/cards/{filename}")
        void validCardSuccess() {
            var res = resolver.forCreate("verified/45/cat.png", SystemFileModule.CARDS);
            assertThat(res.key()).isEqualTo("system/cards/cat.png");
            assertThat(res.verifiedKey()).isEqualTo("verified/45/cat.png");
            assertThat(res.obsoleteKeys()).isEmpty();
        }

        @Test
        @DisplayName("valid verified key for PHONETICS -> system/phonetics/{filename}")
        void validPhoneticsSuccess() {
            var res = resolver.forCreate("verified/99/pronounce.mp3", SystemFileModule.PHONETICS);
            assertThat(res.key()).isEqualTo("system/phonetics/pronounce.mp3");
            assertThat(res.verifiedKey()).isEqualTo("verified/99/pronounce.mp3");
            assertThat(res.obsoleteKeys()).isEmpty();
        }

        @Test
        @DisplayName("null or blank verifiedKey -> EMPTY")
        void nullOrBlank() {
            assertThat(resolver.forCreate(null, SystemFileModule.DECKS).key()).isNull();
            assertThat(resolver.forCreate("   ", SystemFileModule.DECKS).key()).isNull();
        }

        @Test
        @DisplayName("key not in verified zone -> INVALID_REQUEST")
        void notInVerified() {
            assertThatThrownBy(() -> resolver.forCreate("staging/12/banner.png", SystemFileModule.DECKS))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_REQUEST));
        }

        @Test
        @DisplayName("null module -> INVALID_REQUEST")
        void nullModule() {
            assertThatThrownBy(() -> resolver.forCreate("verified/12/banner.png", (SystemFileModule) null))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_REQUEST));
        }

        @Test
        @DisplayName("media kind not supported by module (audio for DECKS) -> INVALID_FILE_TYPE")
        void unsupportedKindForModule() {
            assertThatThrownBy(() -> resolver.forCreate("verified/12/audio.mp3", SystemFileModule.DECKS))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_FILE_TYPE));
        }
    }

    // ========================================================================
    // 3. forUpdate(newKey, oldKey, MediaKind) — User Update
    // ========================================================================
    @Nested
    @DisplayName("forUpdate with MediaKind (User)")
    class ForUpdateUserTests {

        @Test
        @DisplayName("delete file (newKey is null or blank) -> obsolete contains old key")
        void deleteFile() {
            var res = resolver.forUpdate(null, "uploads/12/old.png", MediaKind.IMAGE);
            assertThat(res.key()).isNull();
            assertThat(res.verifiedKey()).isNull();
            assertThat(res.obsoleteKeys()).containsExactly("uploads/12/old.png");
        }

        @Test
        @DisplayName("replace with new verified file -> new uploads key and old key in obsolete")
        void replaceFile() {
            var res = resolver.forUpdate("verified/12/new.png", "uploads/12/old.png", MediaKind.IMAGE);
            assertThat(res.key()).isEqualTo("uploads/12/new.png");
            assertThat(res.verifiedKey()).isEqualTo("verified/12/new.png");
            assertThat(res.obsoleteKeys()).containsExactly("uploads/12/old.png");
        }

        @Test
        @DisplayName("keep existing uploads key unchanged -> no verifiedKey and no obsolete")
        void keepExistingUploadsKey() {
            var res = resolver.forUpdate("uploads/12/same.png", "uploads/12/same.png", MediaKind.IMAGE);
            assertThat(res.key()).isEqualTo("uploads/12/same.png");
            assertThat(res.verifiedKey()).isNull();
            assertThat(res.obsoleteKeys()).isEmpty();
        }

        @Test
        @DisplayName("keep existing system key unchanged -> no verifiedKey and no obsolete")
        void keepExistingSystemKey() {
            var res = resolver.forUpdate("system/decks/shared.png", "system/decks/shared.png", MediaKind.IMAGE);
            assertThat(res.key()).isEqualTo("system/decks/shared.png");
            assertThat(res.verifiedKey()).isNull();
            assertThat(res.obsoleteKeys()).isEmpty();
        }

        @Test
        @DisplayName("different unverified key -> INVALID_REQUEST")
        void differentKeyInvalid() {
            assertThatThrownBy(() -> resolver.forUpdate("uploads/12/other.png", "uploads/12/old.png", MediaKind.IMAGE))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_REQUEST));

            assertThatThrownBy(() -> resolver.forUpdate("staging/12/other.png", "uploads/12/old.png", MediaKind.IMAGE))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_REQUEST));
        }
    }

    // ========================================================================
    // 4. forUpdate(newKey, oldKey, SystemFileModule) — Admin System Update
    // ========================================================================
    @Nested
    @DisplayName("forUpdate with SystemFileModule (Admin)")
    class ForUpdateSystemTests {

        @Test
        @DisplayName("keep existing deep system path (system/decks/common-1000/animals/image/cat.png) -> success")
        void keepDeepSystemPath() {
            String deepSystemKey = "system/decks/common-1000/animals/image/cat.png";
            var res = resolver.forUpdate(deepSystemKey, deepSystemKey, SystemFileModule.DECKS);

            assertThat(res.key()).isEqualTo(deepSystemKey);
            assertThat(res.verifiedKey()).isNull();
            assertThat(res.obsoleteKeys()).isEmpty();
        }

        @Test
        @DisplayName("keep existing standard system path (system/cards/uuid.png) -> success")
        void keepStandardSystemPath() {
            String standardKey = "system/cards/550e8400-e29b-41d4-a716-446655440000.png";
            var res = resolver.forUpdate(standardKey, standardKey, SystemFileModule.CARDS);

            assertThat(res.key()).isEqualTo(standardKey);
            assertThat(res.verifiedKey()).isNull();
            assertThat(res.obsoleteKeys()).isEmpty();
        }

        @Test
        @DisplayName("replace system file with new verified file -> promote to system/{module} and mark old system file as obsolete")
        void replaceSystemFileDeletesOld() {
            String oldSystemKey = "system/decks/common-1000/animals/image/cat.png";
            String newVerified = "verified/12/new_cat.png";
            var res = resolver.forUpdate(newVerified, oldSystemKey, SystemFileModule.DECKS);

            assertThat(res.key()).isEqualTo("system/decks/new_cat.png");
            assertThat(res.verifiedKey()).isEqualTo(newVerified);
            assertThat(res.obsoleteKeys()).containsExactly(oldSystemKey);
        }

        @Test
        @DisplayName("replace user uploads old key -> puts old key into obsolete")
        void replaceUserKeyInSystem() {
            String oldUserKey = "uploads/12/user_banner.png";
            String newVerified = "verified/12/admin_banner.png";
            var res = resolver.forUpdate(newVerified, oldUserKey, SystemFileModule.DECKS);

            assertThat(res.key()).isEqualTo("system/decks/admin_banner.png");
            assertThat(res.verifiedKey()).isEqualTo(newVerified);
            assertThat(res.obsoleteKeys()).containsExactly(oldUserKey);
        }

        @Test
        @DisplayName("delete system file (newKey is null or blank) -> mark old system file as obsolete")
        void deleteSystemFile() {
            String oldSystemKey = "system/decks/common-1000/animals/image/cat.png";
            var res = resolver.forUpdate(null, oldSystemKey, SystemFileModule.DECKS);

            assertThat(res.key()).isNull();
            assertThat(res.verifiedKey()).isNull();
            assertThat(res.obsoleteKeys()).containsExactly(oldSystemKey);
        }

        @Test
        @DisplayName("mismatch media kind for system module -> INVALID_FILE_TYPE")
        void mismatchKind() {
            assertThatThrownBy(() -> resolver.forUpdate("verified/12/sound.mp3", "system/decks/old.png", SystemFileModule.DECKS))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_FILE_TYPE));
        }

        @Test
        @DisplayName("foreign unverified key -> INVALID_REQUEST")
        void foreignKey() {
            assertThatThrownBy(() -> resolver.forUpdate("uploads/12/wrong.png", "system/decks/old.png", SystemFileModule.DECKS))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_REQUEST));
        }
    }

    // ========================================================================
    // 5. publish(Resolved) — Event Dispatching
    // ========================================================================
    @Nested
    @DisplayName("publish")
    class PublishTests {

        @Test
        @DisplayName("system destination key -> SystemFileAttachedEvent")
        void publishesSystemAttached() {
            var res = new MediaKeyResolver.Resolved("system/decks/banner.png", "verified/12/banner.png", List.of());
            resolver.publish(res);

            assertThat(publisher.events).hasSize(1);
            assertThat(publisher.events.getFirst()).isInstanceOf(SystemFileAttachedEvent.class);
            SystemFileAttachedEvent event = (SystemFileAttachedEvent) publisher.events.getFirst();
            assertThat(event.verifiedFileKey()).isEqualTo("verified/12/banner.png");
            assertThat(event.systemFileKey()).isEqualTo("system/decks/banner.png");
        }

        @Test
        @DisplayName("user destination key -> FileAttachedEvent")
        void publishesUserAttached() {
            var res = new MediaKeyResolver.Resolved("uploads/12/avatar.png", "verified/12/avatar.png", List.of());
            resolver.publish(res);

            assertThat(publisher.events).hasSize(1);
            assertThat(publisher.events.getFirst()).isInstanceOf(FileAttachedEvent.class);
            FileAttachedEvent event = (FileAttachedEvent) publisher.events.getFirst();
            assertThat(event.verifiedFileKey()).isEqualTo("verified/12/avatar.png");
            assertThat(event.uploadsFileKey()).isEqualTo("uploads/12/avatar.png");
        }

        @Test
        @DisplayName("verifiedKey is null -> no attached event published")
        void noAttachedEvent() {
            var res = new MediaKeyResolver.Resolved("uploads/12/avatar.png", null, List.of());
            resolver.publish(res);

            assertThat(publisher.events).isEmpty();
        }

        @Test
        @DisplayName("obsoleteKeys present -> FileDeletedEvent published")
        void publishesDeleted() {
            var res = new MediaKeyResolver.Resolved(null, null, List.of("uploads/12/old.png"));
            resolver.publish(res);

            assertThat(publisher.events).hasSize(1);
            assertThat(publisher.events.getFirst()).isInstanceOf(FileDeletedEvent.class);
            FileDeletedEvent event = (FileDeletedEvent) publisher.events.getFirst();
            assertThat(event.fileKeys()).containsExactly("uploads/12/old.png");
        }

        @Test
        @DisplayName("both verifiedKey and obsoleteKeys -> publishes both events")
        void publishesBoth() {
            var res = new MediaKeyResolver.Resolved("system/cards/cat.png", "verified/12/cat.png", List.of("uploads/12/old.png"));
            resolver.publish(res);

            assertThat(publisher.events).hasSize(2);
            assertThat(publisher.events.get(0)).isInstanceOf(SystemFileAttachedEvent.class);
            assertThat(publisher.events.get(1)).isInstanceOf(FileDeletedEvent.class);
        }
    }
}
