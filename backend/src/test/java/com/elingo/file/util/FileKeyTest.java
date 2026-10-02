package com.elingo.file.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class FileKeyTest {

    @Nested
    @DisplayName("Moving a key between zones")
    class ZoneSwitch {

        @Test
        @DisplayName("The zone changes but the file name stays the same")
        void keepsFileNameWhenSwitchingZone() {
            assertThat(FileKey.toVerified("staging/12/9f3a-1111.png"))
                    .isEqualTo("verified/12/9f3a-1111.png");

            // Hai vùng là cùng một file ở hai nơi — đây là điều làm cho việc sửa chữa
            // hậu kỳ chỉ cần biết tên file là dò lại được.
            assertThat(FileKey.toUploads("verified/12/9f3a-1111.png"))
                    .isEqualTo("uploads/12/9f3a-1111.png");
        }

        @Test
        @DisplayName("A key can walk the full path staging → verified → uploads")
        void walksThroughEveryZone() {
            String stagingKey = "staging/12/9f3a-1111.png";

            String verifiedKey = FileKey.toVerified(stagingKey);
            String uploadsKey = FileKey.toUploads(verifiedKey);

            assertThat(uploadsKey).isEqualTo("uploads/12/9f3a-1111.png");
        }

        @Test
        @DisplayName("The extension survives untouched")
        void preservesExtension() {
            assertThat(FileKey.toUploads("verified/7/dead-beef-0000.m4a")).endsWith(".m4a");
        }
    }

    @Nested
    @DisplayName("Rejecting a key that is not in the expected zone")
    class WrongZone {

        @Test
        @DisplayName("A key from the wrong zone is rejected rather than silently rewritten")
        void rejectsKeyFromWrongZone() {
            // Trả về key sai sẽ lọt vào CSDL và hỏng về sau, tệ hơn là ném ra ngay.
            assertThatThrownBy(() -> FileKey.toUploads("staging/12/9f3a-1111.png"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("verified/");

            assertThatThrownBy(() -> FileKey.toVerified("uploads/12/9f3a-1111.png"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("staging/");
        }

        @Test
        @DisplayName("Promoting the same key twice is rejected — it is no longer in verified/")
        void rejectsDoublePromotion() {
            String uploadsKey = FileKey.toUploads("verified/12/9f3a-1111.png");

            assertThatThrownBy(() -> FileKey.toUploads(uploadsKey))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("A zone name used as a prefix of another zone does not count as a match")
        void requiresTheSlashToMatch() {
            // "uploads2/..." bắt đầu bằng chuỗi "uploads" nhưng không phải vùng uploads.
            assertThatThrownBy(() -> FileKey.toUploads("uploads2/12/9f3a-1111.png"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("A bare zone name with no path is rejected")
        void rejectsBareZoneName() {
            assertThatThrownBy(() -> FileKey.toUploads("verified"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Rejecting an unusable key")
    class Unusable {

        @Test
        @DisplayName("null is rejected instead of causing a NullPointerException deep inside")
        void rejectsNull() {
            assertThatThrownBy(() -> FileKey.toUploads(null))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> FileKey.toVerified(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("An empty or blank key is rejected")
        void rejectsBlank() {
            assertThatThrownBy(() -> FileKey.toUploads("")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> FileKey.toUploads("   ")).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
