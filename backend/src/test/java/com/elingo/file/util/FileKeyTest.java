package com.elingo.file.util;

import com.elingo.common.enums.MediaKind;
import com.elingo.common.exception.AppException;
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
        @DisplayName("non-verified key -> AppException")
        void rejectsNonVerified() {
            assertThatThrownBy(() -> FileKey.toUploads("staging/12/abc.png"))
                    .isInstanceOf(AppException.class);
            assertThatThrownBy(() -> FileKey.toUploads("uploads/12/abc.png"))
                    .isInstanceOf(AppException.class);
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
        @DisplayName("correct user in verified zone")
        void correctUserVerified() {
            assertThat(FileKey.isOwnedBy("verified/12/a.png", 12L)).isTrue();
        }

        @Test
        @DisplayName("correct user in uploads zone")
        void correctUserUploads() {
            assertThat(FileKey.isOwnedBy("uploads/12/a.png", 12L)).isTrue();
        }

        @Test
        @DisplayName("wrong user")
        void wrongUser() {
            assertThat(FileKey.isOwnedBy("verified/12/a.png", 99L)).isFalse();
        }

        @Test
        @DisplayName("null userId")
        void nullUserId() {
            assertThat(FileKey.isOwnedBy("verified/12/a.png", null)).isFalse();
        }

        @Test
        @DisplayName("null fileKey")
        void nullFileKey() {
            assertThat(FileKey.isOwnedBy(null, 12L)).isFalse();
        }

        @Test
        @DisplayName("key with path traversal")
        void pathTraversal() {
            assertThat(FileKey.isOwnedBy("verified/12/../a.png", 12L)).isFalse();
        }

        @Test
        @DisplayName("missing user segment")
        void missingUserSegment() {
            assertThat(FileKey.isOwnedBy("verified/a.png", 12L)).isFalse();
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

    @Nested
    @DisplayName("toKey - public URL -> object key")
    class ToKey {

        @Test
        @DisplayName("null value -> null")
        void nullValue() {
            assertThat(FileKey.toKey("https://cdn.elingo.com", null)).isNull();
        }

        @Test
        @DisplayName("blank value -> null")
        void blankValue() {
            assertThat(FileKey.toKey("https://cdn.elingo.com", "   ")).isNull();
        }

        @Test
        @DisplayName("value already a bare key -> returned as-is")
        void bareKey() {
            assertThat(FileKey.toKey("https://cdn.elingo.com", "uploads/12/a.png"))
                    .isEqualTo("uploads/12/a.png");
        }

        @Test
        @DisplayName("value is full URL with matching domain -> key extracted")
        void fullUrlMatchingDomain() {
            assertThat(FileKey.toKey("https://cdn.elingo.com",
                    "https://cdn.elingo.com/uploads/12/a.png"))
                    .isEqualTo("uploads/12/a.png");
        }

        @Test
        @DisplayName("publicUrl has trailing slash, value matches -> key extracted")
        void publicUrlTrailingSlash() {
            assertThat(FileKey.toKey("https://cdn.elingo.com/",
                    "https://cdn.elingo.com/uploads/12/a.png"))
                    .isEqualTo("uploads/12/a.png");
        }

        @Test
        @DisplayName("query string stripped")
        void stripsQueryString() {
            assertThat(FileKey.toKey("https://cdn.elingo.com",
                    "https://cdn.elingo.com/uploads/12/a.png?version=1"))
                    .isEqualTo("uploads/12/a.png");
        }

        @Test
        @DisplayName("fragment stripped")
        void stripsFragment() {
            assertThat(FileKey.toKey("https://cdn.elingo.com",
                    "https://cdn.elingo.com/uploads/12/a.png#section"))
                    .isEqualTo("uploads/12/a.png");
        }

        @Test
        @DisplayName("leading double slashes collapsed")
        void leadingSlashes() {
            assertThat(FileKey.toKey("https://cdn.elingo.com",
                    "https://cdn.elingo.com//uploads/12/a.png"))
                    .isEqualTo("uploads/12/a.png");
        }

        @Test
        @DisplayName("value is full URL of different domain -> AppException")
        void differentDomain() {
            assertThatThrownBy(() -> FileKey.toKey("https://cdn.elingo.com",
                    "https://evil.com/uploads/12/a.png"))
                    .isInstanceOf(AppException.class);
        }

        @Test
        @DisplayName("value reduces to empty after stripping -> null")
        void reducesToEmpty() {
            assertThat(FileKey.toKey("https://cdn.elingo.com",
                    "https://cdn.elingo.com?query"))
                    .isNull();
        }

        @Test
        @DisplayName("bare key with leading slash -> slash stripped")
        void bareKeyWithLeadingSlash() {
            assertThat(FileKey.toKey("https://cdn.elingo.com",
                    "/uploads/12/a.png"))
                    .isEqualTo("uploads/12/a.png");
        }
    }
}