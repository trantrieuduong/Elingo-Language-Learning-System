package com.elingo.community.moderation.service;

import com.elingo.community.moderation.config.ModerationProperties;
import com.elingo.community.moderation.exception.ModerationUnavailableException;
import com.elingo.community.moderation.exception.UnprocessableContentException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.elingo.community.moderation.model.FrameBatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Trích frame JPEG từ video
 */
@Slf4j(topic = "FFMPEG-FRAME-EXTRACTOR")
@Component
public class FfmpegFrameExtractor {
    // ---- Tham số điều chỉnh tại đây ----
    private static final Duration FFMPEG_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration SLOT_WAIT_TIMEOUT = Duration.ofSeconds(60);
    /** Khoảng cách tối thiểu giữa 2 frame được chọn (giây). */
    private static final double MIN_INTERVAL_SECONDS = 2.0;
    /** Dùng khi ffprobe không đọc được duration (giây). */
    private static final double FALLBACK_INTERVAL_SECONDS = 10.0;
    private static final int FRAME_MAX_WIDTH = 512;
    /** Chất lượng JPEG của ffmpeg: 2 (đẹp nhất) đến 31 (kém nhất). */
    private static final int JPEG_QUALITY = 5;
    /** Timeout đọc/ghi mạng của ffmpeg/ffprobe, đơn vị microsecond. */
    private static final String NETWORK_TIMEOUT_MICROS = "15000000";

    private final int maxFrames;
    private final String ffmpegPath;
    private final String ffprobePath;
    private final Semaphore slots;

    public FfmpegFrameExtractor(ModerationProperties props,
                                @Value("${app.ffmpeg.path}") String ffmpegPath,
                                @Value("${app.ffmpeg.ffprobe-path}")String ffprobePath,
                                @Value("${app.ffmpeg.max-concurrent}") int ffmpegMaxConcurrent) {
        this.maxFrames = props.policy().videoMaxFrames();
        this.ffmpegPath = ffmpegPath;
        this.ffprobePath = ffprobePath;
        this.slots = new Semaphore(ffmpegMaxConcurrent, true);
    }

    public FrameBatch extract(String publicUrl) {
        acquireSlot();
        Path dir = null;
        try {
            dir = Files.createTempDirectory("elingo-moderation-frames-");
            double interval = computeInterval(publicUrl);
            runFfmpeg(publicUrl, dir, interval);
            List<Path> frames = listFrames(dir);

            if (frames.isEmpty())
                throw new UnprocessableContentException("No frames could be extracted from the video");

            log.debug("Video frames extracted count={} interval={}", frames.size(), interval);
            return new FrameBatch(dir, frames);
        } catch (IOException e) {
            FrameBatch.deleteRecursively(dir);
            throw new IllegalStateException("Failed to create temp directory for video frames", e);
        } catch (RuntimeException e) {
            FrameBatch.deleteRecursively(dir);
            throw e;
        } finally {
            slots.release(); // chỉ giữ slot trong lúc chạy ffmpeg, không giữ khi gọi OpenAI
        }
    }

    private void acquireSlot() {
        try {
            if (!slots.tryAcquire(SLOT_WAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new ModerationUnavailableException("ffmpeg is busy");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ModerationUnavailableException("Interrupted while waiting for an ffmpeg slot", e);
        }
    }

    /** Lấy duration bằng ffprobe để trải đều frame; lỗi thì dùng khoảng cách mặc định. */
    private double computeInterval(String url) {
        try {
            Process p = new ProcessBuilder(ffprobePath, "-v", "error", "-rw_timeout", NETWORK_TIMEOUT_MICROS,
                    "-show_entries", "format=duration", "-of", "csv=p=0", url)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!p.waitFor(PROBE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                p.destroyForcibly();
                return FALLBACK_INTERVAL_SECONDS;
            }
            double duration = Double.parseDouble(
                    new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim());
            return Math.max(MIN_INTERVAL_SECONDS, duration / maxFrames);
        } catch (IOException | NumberFormatException e) {
            return FALLBACK_INTERVAL_SECONDS;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ModerationUnavailableException("Interrupted while probing the video", e);
        }
    }

    private void runFfmpeg(String url, Path dir, double interval) {
        String filter = String.format(Locale.ROOT,
                "select='isnan(prev_selected_t)+gte(t-prev_selected_t,%.2f)',scale='min(%d,iw)':-2",
                interval, FRAME_MAX_WIDTH);
        List<String> cmd = List.of(
                ffmpegPath, "-nostdin", "-hide_banner", "-loglevel", "error",
                "-threads", "1",
                "-skip_frame", "nokey",
                "-rw_timeout", NETWORK_TIMEOUT_MICROS,
                "-i", url,
                "-an", "-sn",
                "-vf", filter,
                "-fps_mode", "vfr",
                "-frames:v", String.valueOf(maxFrames),
                "-q:v", String.valueOf(JPEG_QUALITY),
                dir.resolve("frame_%03d.jpg").toString());
        Path logFile = dir.resolve("ffmpeg.log");

        Process process = null;
        try {
            process = new ProcessBuilder(cmd).redirectErrorStream(true).redirectOutput(logFile.toFile()).start();
            if (!process.waitFor(FFMPEG_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new UnprocessableContentException("ffmpeg timed out after " + FFMPEG_TIMEOUT);
            }
            if (process.exitValue() != 0) {
                throw new UnprocessableContentException(
                        "ffmpeg failed (exit " + process.exitValue() + "): " + tail(logFile));
            }
        } catch (IOException e) {
            throw new ModerationUnavailableException("Could not start ffmpeg: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new ModerationUnavailableException("Interrupted while running ffmpeg", e);
        }
    }

    private static List<Path> listFrames(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(f -> f.getFileName().toString().endsWith(".jpg")).sorted().toList();
        }
    }

    private static String tail(Path logFile) {
        try {
            String s = Files.readString(logFile).trim();
            return s.length() > 300 ? s.substring(s.length() - 300) : s;
        } catch (IOException e) {
            return "(ffmpeg log unavailable)";
        }
    }
}
