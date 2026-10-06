package com.elingo.file.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm tra bảng magic bytes và chuẩn hoá Content-Type.
 *
 * <p>{@code detect} chỉ trả về mime type nằm trong allowlist của {@link com.elingo.common.enums.MediaKind} —
 * dự án không hỗ trợ SVG nên chúng không có chữ ký riêng.
 */
@Tag("unit")
class FileSignatureVerifierTest {

    @Nested
    @DisplayName("detect - JPEG/PNG/GIF basic")
    class BasicImages {
        @Test
        @DisplayName("JPEG via first 3 bytes FF D8 FF")
        void jpeg() {
            byte[] header = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x00};
            assertThat(FileSignatureVerifier.detect(header)).isEqualTo("image/jpeg");
        }

        @Test
        @DisplayName("PNG via standard 8-byte header")
        void png() {
            byte[] header = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
            assertThat(FileSignatureVerifier.detect(header)).isEqualTo("image/png");
        }

        @Test
        @DisplayName("GIF87a")
        void gif87a() {
            byte[] header = {0x47, 0x49, 0x46, 0x38, 0x37, 0x61, 0x00};
            assertThat(FileSignatureVerifier.detect(header)).isEqualTo("image/gif");
        }

        @Test
        @DisplayName("GIF89a")
        void gif89a() {
            byte[] header = {0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x00};
            assertThat(FileSignatureVerifier.detect(header)).isEqualTo("image/gif");
        }
    }

    @Nested
    @DisplayName("detect - RIFF (WAV/AVI/WebP)")
    class Riff {
        @Test
        @DisplayName("WAV via offset 8")
        void wav() {
            byte[] header = new byte[12];
            header[0] = 0x52; header[1] = 0x49; header[2] = 0x46; header[3] = 0x46; // RIFF
            header[8] = 0x57; header[9] = 0x41; header[10] = 0x56; header[11] = 0x45; // WAVE
            assertThat(FileSignatureVerifier.detect(header)).isEqualTo("audio/wav");
        }

        @Test
        @DisplayName("AVI via offset 8")
        void avi() {
            byte[] header = new byte[12];
            header[0] = 0x52; header[1] = 0x49; header[2] = 0x46; header[3] = 0x46;
            header[8] = 0x41; header[9] = 0x56; header[10] = 0x49; header[11] = 0x20; // "AVI "
            assertThat(FileSignatureVerifier.detect(header)).isEqualTo("video/x-msvideo");
        }

        @Test
        @DisplayName("WebP via offset 8")
        void webp() {
            byte[] header = new byte[12];
            header[0] = 0x52; header[1] = 0x49; header[2] = 0x46; header[3] = 0x46;
            header[8] = 0x57; header[9] = 0x45; header[10] = 0x42; header[11] = 0x50; // WEBP
            assertThat(FileSignatureVerifier.detect(header)).isEqualTo("image/webp");
        }

        @Test
        @DisplayName("RIFF with unknown subtype -> null")
        void riffUnknownSubtype() {
            byte[] header = new byte[12];
            header[0] = 0x52; header[1] = 0x49; header[2] = 0x46; header[3] = 0x46;
            header[8] = 0x58; header[9] = 0x58; header[10] = 0x58; header[11] = 0x58;
            assertThat(FileSignatureVerifier.detect(header)).isNull();
        }
    }

    @Nested
    @DisplayName("detect - EBML (Matroska/WebM)")
    class Ebml {
        @Test
        @DisplayName("WebM")
        void webm() {
            assertThat(FileSignatureVerifier.detect(buildEbml("webm"))).isEqualTo("video/webm");
        }

        @Test
        @DisplayName("Matroska")
        void matroska() {
            assertThat(FileSignatureVerifier.detect(buildEbml("matroska"))).isEqualTo("video/x-matroska");
        }

        /**
         * Xây dựng header 64 byte chứa EBML header hợp lệ + DocType.
         * Định dạng vint theo đúng quy tắc parser (xem {@link FileSignatureVerifier}):
         * byte >= 0x80 → 1-byte vint (giá trị = byte & 0x7F);
         * byte < 0x80 → multi-byte, length = leadingZeros(byte) - 23.
         */
        private byte[] buildEbml(String docType) {
            byte[] header = new byte[64];
            int pos = 0;

            // EBML header ID = 0x1A45DFA3 -> 4-byte vint
            header[pos++] = 0x1A; header[pos++] = 0x45; header[pos++] = (byte) 0xDF; header[pos++] = (byte) 0xA3;

            // EBML header size -> 1-byte vint 0x80|size
            int docTypeLen = docType.length();
            int innerSize = 4 + 4 + (3 + docTypeLen); // EBMLVersion + EBMLReadVersion + DocType
            header[pos++] = (byte) (0x80 | innerSize);

            // EBMLVersion (ID 0x4286, size 1, value 1)
            header[pos++] = 0x42; header[pos++] = (byte) 0x86; // ID
            header[pos++] = (byte) 0x81; header[pos++] = 0x01; // size=1, value=1

            // EBMLReadVersion (ID 0x4287, size 1, value 1)
            header[pos++] = 0x42; header[pos++] = (byte) 0x87;
            header[pos++] = (byte) 0x81; header[pos++] = 0x01;

            // DocType (ID 0x4282, size=docTypeLen, value=docType)
            header[pos++] = 0x42; header[pos++] = (byte) 0x82; // ID
            header[pos++] = (byte) (0x80 | docTypeLen);       // size
            System.arraycopy(docType.getBytes(java.nio.charset.StandardCharsets.US_ASCII), 0, header, pos, docTypeLen);

            return header;
        }
    }

    @Nested
    @DisplayName("detect - MP3")
    class Mp3 {
        @Test
        @DisplayName("MP3 via frame sync FF Fx")
        void mp3() {
            byte[] header = {(byte) 0xFF, (byte) 0xFB, 0x00, 0x00};
            assertThat(FileSignatureVerifier.detect(header)).isEqualTo("audio/mpeg");
        }

        @Test
        @DisplayName("ID3 header returns audio/mpeg")
        void id3() {
            byte[] header = {0x49, 0x44, 0x33, 0x00, 0x00, 0x00};
            assertThat(FileSignatureVerifier.detect(header)).isEqualTo("audio/mpeg");
        }

        @Test
        @DisplayName("first byte FF but second byte not frame sync -> null")
        void notMp3FrameSync() {
            byte[] header = {(byte) 0xFF, 0x00};
            assertThat(FileSignatureVerifier.detect(header)).isNull();
        }
    }

    @Nested
    @DisplayName("detect - ISO-BMFF (MP4/MOV/M4A)")
    class IsoBmff {
        @Test
        @DisplayName("MP4 via brand 'isom' at offset 8")
        void mp4() {
            byte[] header = new byte[12];
            header[4] = 0x66; header[5] = 0x74; header[6] = 0x79; header[7] = 0x70; // ftyp
            header[8] = 0x69; header[9] = 0x73; header[10] = 0x6F; header[11] = 0x6D; // isom
            assertThat(FileSignatureVerifier.detect(header)).isEqualTo("video/mp4");
        }

        @Test
        @DisplayName("MOV via brand 'qt  '")
        void mov() {
            byte[] header = new byte[12];
            header[4] = 0x66; header[5] = 0x74; header[6] = 0x79; header[7] = 0x70;
            header[8] = 0x71; header[9] = 0x74; header[10] = 0x20; header[11] = 0x20; // "qt  "
            assertThat(FileSignatureVerifier.detect(header)).isEqualTo("video/quicktime");
        }

        @Test
        @DisplayName("M4A via brand 'M4A '")
        void m4a() {
            byte[] header = new byte[12];
            header[4] = 0x66; header[5] = 0x74; header[6] = 0x79; header[7] = 0x70;
            header[8] = 0x4D; header[9] = 0x34; header[10] = 0x41; header[11] = 0x20; // "M4A "
            assertThat(FileSignatureVerifier.detect(header)).isEqualTo("audio/mp4");
        }

        @Test
        @DisplayName("ftyp but unknown brand -> null")
        void unknownBrand() {
            byte[] header = new byte[12];
            header[4] = 0x66; header[5] = 0x74; header[6] = 0x79; header[7] = 0x70;
            header[8] = 0x7A; header[9] = 0x7A; header[10] = 0x7A; header[11] = 0x7A; // zzzz
            assertThat(FileSignatureVerifier.detect(header)).isNull();
        }
    }

    @Nested
    @DisplayName("detect - edge cases")
    class EdgeCases {
        @Test
        @DisplayName("empty array -> null")
        void empty() {
            assertThat(FileSignatureVerifier.detect(new byte[0])).isNull();
        }

        @Test
        @DisplayName("null -> null")
        void nullInput() {
            assertThat(FileSignatureVerifier.detect(null)).isNull();
        }

        @Test
        @DisplayName("unrecognized bytes -> null")
        void unknownSignature() {
            byte[] header = {0x00, 0x01, 0x02, 0x03};
            assertThat(FileSignatureVerifier.detect(header)).isNull();
        }

        @Test
        @DisplayName("OGG via header 0x4F 67 67 53")
        void ogg() {
            byte[] header = {0x4F, 0x67, 0x67, 0x53, 0x00, 0x00};
            assertThat(FileSignatureVerifier.detect(header)).isEqualTo("audio/ogg");
        }
    }

    @Nested
    @DisplayName("normalizeDeclaredType")
    class NormalizeDeclaredType {
        @Test
        @DisplayName("strip charset parameter")
        void stripCharset() {
            assertThat(FileSignatureVerifier.normalizeDeclaredType("image/png;charset=UTF-8"))
                    .isEqualTo("image/png");
        }

        @Test
        @DisplayName("lowercase and trim")
        void lowerTrim() {
            assertThat(FileSignatureVerifier.normalizeDeclaredType("  IMAGE/JPEG  "))
                    .isEqualTo("image/jpeg");
        }

        @Test
        @DisplayName("null -> null")
        void nullInput() {
            assertThat(FileSignatureVerifier.normalizeDeclaredType(null)).isNull();
        }

        @Test
        @DisplayName("normalized matches detect output")
        void consistentWithDetect() {
            assertThat(FileSignatureVerifier.normalizeDeclaredType("IMAGE/PNG"))
                    .isEqualTo(FileSignatureVerifier.detect(
                            new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}));
        }
    }

    @Nested
    @DisplayName("isAllowedFileType")
    class IsAllowedFileType {
        @Test
        @DisplayName("allowed mime in allowlist")
        void allowed() {
            assertThat(FileSignatureVerifier.isAllowedFileType("image/png")).isTrue();
            assertThat(FileSignatureVerifier.isAllowedFileType("video/mp4")).isTrue();
            assertThat(FileSignatureVerifier.isAllowedFileType("audio/mpeg")).isTrue();
        }

        @Test
        @DisplayName("mime with charset parameter still allowed")
        void allowedWithCharset() {
            assertThat(FileSignatureVerifier.isAllowedFileType("image/png;charset=UTF-8")).isTrue();
        }

        @Test
        @DisplayName("unsupported mime -> false")
        void notAllowed() {
            assertThat(FileSignatureVerifier.isAllowedFileType("image/svg+xml")).isFalse();
            assertThat(FileSignatureVerifier.isAllowedFileType("application/pdf")).isFalse();
            assertThat(FileSignatureVerifier.isAllowedFileType("text/plain")).isFalse();
        }
    }
}