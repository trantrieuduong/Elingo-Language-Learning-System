package com.elingo.file.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class FileSignatureVerifierTest {

    /** Dựng header giả: các byte cho trước, phần còn lại zero-pad. */
    private static byte[] header(byte[]... parts) {
        int size = 0;
        for (byte[] part : parts) {
            size += part.length;
        }
        byte[] result = new byte[Math.max(size, FileSignatureVerifier.SIGNATURE_LENGTH)];
        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, result, offset, part.length);
            offset += part.length;
        }
        return result;
    }

    private static byte[] ascii(String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }

    /** RIFF: RIFF + 4 byte size + subtype ở offset 8. */
    private static byte[] riff(String subtype) {
        return header(ascii("RIFF"), new byte[4], ascii(subtype));
    }

    /** ISO-BMFF: 4 byte size rồi tới 'ftyp' ở offset 4, brand ở offset 8. */
    private static byte[] isoBmff(String brand) {
        return header(new byte[4], ascii("ftyp"), ascii(brand));
    }

    @Nested
    @DisplayName("Image formats")
    class Images {

        @Test
        @DisplayName("JPEG is detected by its 3-byte SOI marker")
        void detectsJpeg() {
            assertThat(FileSignatureVerifier.detect(
                    header(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}))).isEqualTo("image/jpeg");
        }

        @Test
        @DisplayName("PNG is detected by its 8-byte signature")
        void detectsPng() {
            assertThat(FileSignatureVerifier.detect(
                    header(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}))).isEqualTo("image/png");
        }

        @Test
        @DisplayName("GIF is detected in both the 87a and 89a flavours")
        void detectsGif() {
            assertThat(FileSignatureVerifier.detect(header(ascii("GIF89a")))).isEqualTo("image/gif");
            assertThat(FileSignatureVerifier.detect(header(ascii("GIF87a")))).isEqualTo("image/gif");
        }
    }

    @Nested
    @DisplayName("RIFF — shared signature, distinguished by subtype")
    class Riff {

        @Test
        @DisplayName("Subtype WAVE yields audio/wav")
        void detectsWavBySubtype() {
            assertThat(FileSignatureVerifier.detect(riff("WAVE"))).isEqualTo("audio/wav");
        }

        @Test
        @DisplayName("Subtype AVI yields video/x-msvideo — same first 4 bytes as WAV")
        void distinguishesAviFromWavBySubtype() {
            byte[] wav = riff("WAVE");
            byte[] avi = riff("AVI ");

            assertThat(wav).startsWith(avi[0], avi[1], avi[2], avi[3]);
            assertThat(FileSignatureVerifier.detect(avi)).isEqualTo("video/x-msvideo");
        }

        @Test
        @DisplayName("A RIFF file with an unknown subtype is not accepted")
        void rejectsUnknownRiffSubtype() {
            assertThat(FileSignatureVerifier.detect(riff("WEBP"))).isNull();
        }
    }

    @Nested
    @DisplayName("ISO-BMFF — MP4 / MOV / M4A distinguished by brand")
    class IsoBmff {

        @Test
        @DisplayName("Brand qt yields video/quicktime")
        void detectsQuickTime() {
            assertThat(FileSignatureVerifier.detect(isoBmff("qt  "))).isEqualTo("video/quicktime");
        }

        @Test
        @DisplayName("Brands isom and mp42 yield video/mp4")
        void detectsMp4() {
            assertThat(FileSignatureVerifier.detect(isoBmff("isom"))).isEqualTo("video/mp4");
            assertThat(FileSignatureVerifier.detect(isoBmff("mp42"))).isEqualTo("video/mp4");
        }

        @Test
        @DisplayName("Brand M4A yields audio/mp4, not video/mp4")
        void distinguishesM4aFromMp4ByBrand() {
            assertThat(FileSignatureVerifier.detect(isoBmff("M4A "))).isEqualTo("audio/mp4");
        }
    }

    @Nested
    @DisplayName("EBML — WebM and Matroska distinguished by DocType")
    class Ebml {

        @Test
        @DisplayName("DocType webm yields video/webm")
        void detectsWebm() {
            assertThat(FileSignatureVerifier.detect(
                    header(new byte[]{0x1A, 0x45, (byte) 0xDF, (byte) 0xA3}, ascii("webm"))))
                    .isEqualTo("video/webm");
        }

        @Test
        @DisplayName("DocType matroska yields video/x-matroska")
        void detectsMatroska() {
            assertThat(FileSignatureVerifier.detect(
                    header(new byte[]{0x1A, 0x45, (byte) 0xDF, (byte) 0xA3}, ascii("matroska"))))
                    .isEqualTo("video/x-matroska");
        }
    }

    @Nested
    @DisplayName("Audio formats")
    class Audio {

        @Test
        @DisplayName("MP3 is detected via its ID3 tag")
        void detectsMp3WithId3Tag() {
            assertThat(FileSignatureVerifier.detect(header(ascii("ID3"), new byte[]{0x03, 0x00})))
                    .isEqualTo("audio/mpeg");
        }

        @Test
        @DisplayName("MP3 is detected via its 11-bit frame sync")
        void detectsMp3WithFrameSync() {
            assertThat(FileSignatureVerifier.detect(
                    header(new byte[]{(byte) 0xFF, (byte) 0xFB, (byte) 0x90, 0x00})))
                    .isEqualTo("audio/mpeg");
        }

        @Test
        @DisplayName("Ogg is detected via its OggS signature")
        void detectsOgg() {
            assertThat(FileSignatureVerifier.detect(header(ascii("OggS")))).isEqualTo("audio/ogg");
        }
    }

    @Nested
    @DisplayName("Rejected files")
    class Rejected {

        @Test
        @DisplayName("A Windows PE renamed to .png is still rejected")
        void rejectsExecutableRenamedAsImage() {
            byte[] executable = header(ascii("MZ"), new byte[126]);

            assertThat(FileSignatureVerifier.detect(executable)).isNull();
            assertThat(FileSignatureVerifier.isAllowedFileType(FileSignatureVerifier.detect(executable))).isFalse();
        }

        @Test
        @DisplayName("A file declared PNG but actually JPEG is detected as a mismatch")
        void detectsMismatchedContent() {
            byte[] actualJpeg = header(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00});

            String detected = FileSignatureVerifier.detect(actualJpeg);

            assertThat(detected).isEqualTo("image/jpeg");
            assertThat(detected).isNotEqualTo("image/png");
        }

        @Test
        @DisplayName("PDF and ZIP are not in the allowlist")
        void rejectsNonMediaFormats() {
            assertThat(FileSignatureVerifier.detect(header(ascii("%PDF-1.7")))).isNull();
            assertThat(FileSignatureVerifier.detect(header(new byte[]{0x50, 0x4B, 0x03, 0x04}))).isNull();
        }

        @Test
        @DisplayName("An empty or null header returns null instead of throwing")
        void handlesEmptyHeader() {
            assertThat(FileSignatureVerifier.detect(new byte[0])).isNull();
            assertThat(FileSignatureVerifier.detect(null)).isNull();
        }

        @Test
        @DisplayName("A header shorter than the signature does not throw")
        void handlesTruncatedHeader() {
            assertThat(FileSignatureVerifier.detect(new byte[]{0x47, 0x49, 0x46})).isNull();
        }
    }

    @Nested
    @DisplayName("Normalizing the client-declared Content-Type")
    class Normalize {

        @Test
        @DisplayName("Parameters are stripped and the type is lowercased")
        void stripsParametersAndLowercases() {
            assertThat(FileSignatureVerifier.normalizeDeclaredType("image/PNG")).isEqualTo("image/png");
            assertThat(FileSignatureVerifier.normalizeDeclaredType("image/png; charset=utf-8"))
                    .isEqualTo("image/png");
            assertThat(FileSignatureVerifier.normalizeDeclaredType("audio/ogg; codecs=opus"))
                    .isEqualTo("audio/ogg");
        }

        @Test
        @DisplayName("A normalized declaration matches the mime detected from magic bytes")
        void normalizedDeclarationMatchesDetectedType() {
            String declared = FileSignatureVerifier.normalizeDeclaredType("image/JPEG; charset=binary");
            String detected = FileSignatureVerifier.detect(
                    header(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}));

            assertThat(detected).isEqualTo(declared);
        }

        @Test
        @DisplayName("SVG is not in the allowlist")
        void svgIsNotAllowed() {
            assertThat(FileSignatureVerifier.isAllowedFileType("image/svg+xml")).isFalse();
        }
    }

    @Nested
    @DisplayName("Deriving the file extension")
    class Extensions {

        @Test
        @DisplayName("Every supported mime maps to its own extension")
        void mapsEveryAllowedTypeToExtension() {
            for (String mime : FileSignatureVerifier.ALLOWED_FILE_TYPES) {
                String extension = FileSignatureVerifier.extensionOf(mime);

                assertThat(extension).isNotEqualTo("bin");
                assertThat(extension).matches("[a-z0-9]+");
            }
        }

        @Test
        @DisplayName("An unknown mime falls back to bin")
        void fallsBackToBin() {
            assertThat(FileSignatureVerifier.extensionOf("application/x-does-not-exist")).isEqualTo("bin");
            assertThat(FileSignatureVerifier.extensionOf(null)).isEqualTo("bin");
        }
    }
}
