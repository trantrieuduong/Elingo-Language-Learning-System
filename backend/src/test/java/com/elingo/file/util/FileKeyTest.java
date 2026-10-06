package com.elingo.file.util;

import com.elingo.common.enums.MediaKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class FileKeyTest {

    @Nested
    @DisplayName("toUploads - verified/ -> uploads/ keeps userId and filename")
    class ToUploads {
        @Test
        @DisplayName("verified key -> uploads key")
        void keepsUserAndName() {
            assertThat(FileKey.toUploads("verified/12/abc.png"))
                    .isEqualTo("uploads/12/abc.png");
        }

        @Test
        @DisplayName("non-verified key -> IllegalArgumentException")
        void rejectsNonVerified() {
            assertThatThrownBy(() -> FileKey.toUploads("staging/12/abc.png"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> FileKey.toUploads("uploads/12/abc.png"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("mediaKindOf - infer group from extension")
    class MediaKindOf {
        @Test
        @DisplayName("known extensions map to correct group")
        void fromExtension() {
            assertThat(FileKey.mediaKindOf("uploads/12/a.png")).isEqualTo(MediaKind.IMAGE);
            assertThat(FileKey.mediaKindOf("verified/12/a.mp4")).isEqualTo(MediaKind.VIDEO);
            assertThat(FileKey.mediaKindOf("staging/12/a.mp3")).isEqualTo(MediaKind.AUDIO);
        }

        @Test
        @DisplayName("no extension -> null")
        void noExtension() {
            assertThat(FileKey.mediaKindOf("uploads/12/abc")).isNull();
        }

        @Test
        @DisplayName("trailing dot -> null")
        void trailingDot() {
            assertThat(FileKey.mediaKindOf("uploads/12/abc.")).isNull();
        }

        @Test
        @DisplayName("unknown extension -> null")
        void unknownExtension() {
            assertThat(FileKey.mediaKindOf("uploads/12/abc.exe")).isNull();
        }

        @Test
        @DisplayName("null -> null")
        void nullInput() {
            assertThat(FileKey.mediaKindOf(null)).isNull();
        }

        @Test
        @DisplayName("filename without path still reads extension")
        void noSlash() {
            assertThat(FileKey.mediaKindOf("a.png")).isEqualTo(MediaKind.IMAGE);
        }
    }

    @Nested
    @DisplayName("isInZone - key starts with zone/")
    class IsInZone {
        @Test
        @DisplayName("correct zone")
        void inZone() {
            assertThat(FileKey.isInZone("verified/12/a.png", FileKey.VERIFIED_PREFIX)).isTrue();
            assertThat(FileKey.isInZone("staging/12/a.png", FileKey.STAGING_PREFIX)).isTrue();
        }

        @Test
        @DisplayName("wrong zone")
        void wrongZone() {
            assertThat(FileKey.isInZone("uploads/12/a.png", FileKey.VERIFIED_PREFIX)).isFalse();
        }

        @Test
        @DisplayName("null key -> false")
        void nullKey() {
            assertThat(FileKey.isInZone(null, FileKey.VERIFIED_PREFIX)).isFalse();
        }

        @Test
        @DisplayName("prefix without trailing slash does not match")
        void prefixBoundary() {
            assertThat(FileKey.isInZone("verifiedX/12/a.png", FileKey.VERIFIED_PREFIX)).isFalse();
        }
    }

    @Nested
    @DisplayName("isOwnedBy - key belongs to userId in zone")
    class IsOwnedBy {
        @Test
        @DisplayName("owned")
        void owned() {
            assertThat(FileKey.isOwnedBy("staging/12/a.png", FileKey.STAGING_PREFIX, 12L)).isTrue();
        }

        @Test
        @DisplayName("different user -> false (IDOR protection)")
        void otherUser() {
            assertThat(FileKey.isOwnedBy("staging/99/a.png", FileKey.STAGING_PREFIX, 12L)).isFalse();
        }

        @Test
        @DisplayName("userId prefix collision (1 vs 12) does not match")
        void prefixUserIdNotConfused() {
            assertThat(FileKey.isOwnedBy("staging/12/a.png", FileKey.STAGING_PREFIX, 1L)).isFalse();
        }

        @Test
        @DisplayName("wrong zone -> false")
        void wrongZone() {
            assertThat(FileKey.isOwnedBy("verified/12/a.png", FileKey.STAGING_PREFIX, 12L)).isFalse();
        }

        @Test
        @DisplayName("userId null -> false")
        void nullUser() {
            assertThat(FileKey.isOwnedBy("staging/12/a.png", FileKey.STAGING_PREFIX, null)).isFalse();
        }

        @Test
        @DisplayName("key null -> false")
        void nullKey() {
            assertThat(FileKey.isOwnedBy(null, FileKey.STAGING_PREFIX, 12L)).isFalse();
        }
    }

    @Nested
    @DisplayName("toPublicUrl - CDN domain + key")
    class ToPublicUrl {
        @Test
        @DisplayName("domain without trailing slash")
        void noTrailingSlash() {
            assertThat(FileKey.toPublicUrl("https://cdn.elingo.com", "uploads/12/a.png"))
                    .isEqualTo("https://cdn.elingo.com/uploads/12/a.png");
        }

        @Test
        @DisplayName("domain with trailing slash - no double slash")
        void trailingSlash() {
            assertThat(FileKey.toPublicUrl("https://cdn.elingo.com/", "uploads/12/a.png"))
                    .isEqualTo("https://cdn.elingo.com/uploads/12/a.png");
        }
    }
}