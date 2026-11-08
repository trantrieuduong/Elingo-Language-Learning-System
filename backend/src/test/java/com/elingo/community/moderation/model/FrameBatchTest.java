package com.elingo.community.moderation.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class FrameBatchTest {

    // ========================================================================
    // read
    // ========================================================================
    @Nested
    @DisplayName("read")
    class ReadTests {

        @Test
        @DisplayName("read() should read all bytes from existing file")
        void read_WhenFileExists_ReadsBytes(@TempDir Path tempDir) throws IOException {
            Path file = tempDir.resolve("sample.jpg");
            byte[] expectedBytes = new byte[]{1, 2, 3, 4, 5};
            Files.write(file, expectedBytes);

            byte[] actualBytes = FrameBatch.read(file);

            assertThat(actualBytes).isEqualTo(expectedBytes);
        }

        @Test
        @DisplayName("read() should throw UncheckedIOException when file does not exist")
        void read_WhenFileDoesNotExist_ThrowsUncheckedIOException(@TempDir Path tempDir) {
            Path nonExistent = tempDir.resolve("missing.jpg");

            assertThatThrownBy(() -> FrameBatch.read(nonExistent))
                    .isInstanceOf(UncheckedIOException.class);
        }
    }

    // ========================================================================
    // close
    // ========================================================================
    @Nested
    @DisplayName("close")
    class CloseTests {

        @Test
        @DisplayName("close() should delete the entire directory and its contents")
        void close_DeletesDirectoryAndFiles(@TempDir Path tempDir) throws IOException {
            Path batchDir = Files.createDirectory(tempDir.resolve("batch-test"));
            Path frame1 = Files.write(batchDir.resolve("frame_001.jpg"), new byte[]{1, 2});
            Path frame2 = Files.write(batchDir.resolve("frame_002.jpg"), new byte[]{3, 4});

            FrameBatch batch = new FrameBatch(batchDir, List.of(frame1, frame2));
            assertThat(batch.getFrames()).containsExactly(frame1, frame2);
            assertThat(Files.exists(batchDir)).isTrue();

            batch.close();

            assertThat(Files.exists(batchDir)).isFalse();
            assertThat(Files.exists(frame1)).isFalse();
            assertThat(Files.exists(frame2)).isFalse();
        }
    }

    // ========================================================================
    // deleteRecursively
    // ========================================================================
    @Nested
    @DisplayName("deleteRecursively")
    class DeleteRecursivelyTests {

        @Test
        @DisplayName("deleteRecursively() should handle null safely without throwing")
        void deleteRecursively_WhenNull_DoesNotThrow() {
            FrameBatch.deleteRecursively(null);
        }
    }
}
