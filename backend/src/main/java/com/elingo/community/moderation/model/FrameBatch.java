package com.elingo.community.moderation.model;

import lombok.Getter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Các frame JPEG đã trích ra thư mục tạm. close() xóa cả thư mục. */
public final class FrameBatch implements AutoCloseable {

    private final Path dir;
    @Getter
    private final List<Path> frames;

    public FrameBatch(Path dir, List<Path> frames) {
        this.dir = dir;
        this.frames = frames;
    }

    public static byte[] read(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        deleteRecursively(dir);
    }

    public static void deleteRecursively(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
            // thư mục tạm sẽ bị hệ điều hành xóa sau
        }
    }
}
