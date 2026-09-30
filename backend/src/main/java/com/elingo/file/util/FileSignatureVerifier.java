package com.elingo.file.util;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Suy ra mime type từ chữ ký nhị phân (magic bytes) của file, không tin vào
 * Content-Type hay đuôi file do client khai.
 *
 * <p>Đọc tối thiểu {@value #SIGNATURE_LENGTH} byte đầu file. Ba định dạng container
 * bắt buộc phải đọc thêm ngoài 4 byte đầu:
 * <ul>
 *   <li><b>RIFF</b> — offset 8 phân biệt WAV và AVI (cùng chữ ký {@code RIFF}).</li>
 *   <li><b>ISO-BMFF</b> — offset 4 {@code ftyp} rồi đọc brand phân biệt MP4, MOV, M4A.</li>
 *   <li><b>EBML</b> — đọc tên doc type phân biệt WebM và Matroska.</li>
 * </ul>
 *
 * <p>Định dạng không khớp bảng trả về {@code null} — coi như không được phép upload.
 * Lưu ý {@code image/svg+xml} không có chữ ký riêng nên không nằm trong bảng này.
 */
public final class FileSignatureVerifier {

    /** Số byte đầu cần đọc để dò được mọi định dạng trong bảng. */
    public static final int SIGNATURE_LENGTH = 64;

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] GIF87A = {0x47, 0x49, 0x46, 0x38, 0x37, 0x61};
    private static final byte[] GIF89A = {0x47, 0x49, 0x46, 0x38, 0x39, 0x61};
    private static final byte[] RIFF = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] EBML = {0x1A, 0x45, (byte) 0xDF, (byte) 0xA3};
    private static final byte[] ID3 = {0x49, 0x44, 0x33};
    private static final byte[] OGG = {0x4F, 0x67, 0x67, 0x53};
    private static final byte[] MP3_FRAME_SYNC = {(byte) 0xFF};
    private static final byte[] FTYP = {0x66, 0x74, 0x79, 0x70};

    private static final int RIFF_SUBTYPE_OFFSET = 8;
    private static final int FTYP_OFFSET = 4;
    private static final int FTYP_BRAND_OFFSET = 8;
    private static final int EBML_DOCTYPE_OFFSET = 4;

    private static final String WAVE_SIGNATURE = "WAVE";
    private static final String AVI_SIGNATURE = "AVI ";

    private static final Map<String, String> RIFF_SUBTYPES = Map.of(
            WAVE_SIGNATURE, "audio/wav",
            AVI_SIGNATURE, "video/x-msvideo"
    );

    private static final Map<String, String> ISO_BMFF_BRANDS = Map.of(
            "qt  ", "video/quicktime",
            "isom", "video/mp4",
            "iso2", "video/mp4",
            "mp41", "video/mp4",
            "mp42", "video/mp4",
            "avc1", "video/mp4",
            "M4A ", "audio/mp4",
            "M4B ", "audio/mp4"
    );

    private static final Map<String, String> EBML_DOCTYPES = Map.of(
            "webm", "video/webm",
            "matroska", "video/x-matroska"
    );

    public static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif"
    );

    public static final List<String> ALLOWED_VIDEO_TYPES = List.of(
            "video/mp4", "video/quicktime", "video/x-msvideo",
            "video/webm", "video/x-matroska"
    );

    public static final List<String> ALLOWED_AUDIO_TYPES = List.of(
            "audio/mpeg", "audio/wav", "audio/ogg", "audio/mp4"
    );

    public static final List<String> ALLOWED_FILE_TYPES =
            java.util.stream.Stream.of(ALLOWED_IMAGE_TYPES, ALLOWED_VIDEO_TYPES, ALLOWED_AUDIO_TYPES)
                    .flatMap(List::stream)
                    .toList();

    private FileSignatureVerifier() {
    }

    /**
     * Dò mime type từ các byte đầu file.
     *
     * @return mime type chuẩn hoá, hoặc {@code null} nếu không khớp định dạng nào được hỗ trợ
     */
    public static String detect(byte[] header) {
        if (header == null || header.length == 0) {
            return null;
        }

        if (startsWith(header, JPEG)) {
            return "image/jpeg";
        }
        if (startsWith(header, PNG)) {
            return "image/png";
        }
        if (startsWith(header, GIF87A) || startsWith(header, GIF89A)) {
            return "image/gif";
        }
        if (startsWith(header, RIFF)) {
            return RIFF_SUBTYPES.get(readAscii(header, RIFF_SUBTYPE_OFFSET, 4));
        }
        if (startsWith(header, EBML)) {
            return EBML_DOCTYPES.get(readDocType(header));
        }
        if (startsWith(header, ID3)) {
            return "audio/mpeg";
        }
        if (startsWith(header, OGG)) {
            return "audio/ogg";
        }
        if (isMp3FrameSync(header)) {
            return "audio/mpeg";
        }
        if (matchesAt(header, FTYP_OFFSET, FTYP)) {
            return ISO_BMFF_BRANDS.get(readAscii(header, FTYP_BRAND_OFFSET, 4));
        }
        return null;
    }

    /**
     * Chuẩn hoá Content-Type do client khai: bỏ phần tham số ({@code ;charset=...},
     * {@code ;codecs=...}) và hạ chữ thường, để so khớp với kết quả {@link #detect}.
     */
    public static String normalizeDeclaredType(String contentType) {
        if (contentType == null) {
            return null;
        }
        int separator = contentType.indexOf(';');
        String base = separator >= 0 ? contentType.substring(0, separator) : contentType;
        return base.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isAllowedFileType(String contentType) {
        return contentType != null && ALLOWED_FILE_TYPES.contains(contentType);
    }

    public static boolean isImageType(String contentType) {
        return contentType != null && ALLOWED_IMAGE_TYPES.contains(contentType);
    }

    public static boolean isVideoType(String contentType) {
        return contentType != null && ALLOWED_VIDEO_TYPES.contains(contentType);
    }

    public static boolean isAudioType(String contentType) {
        return contentType != null && ALLOWED_AUDIO_TYPES.contains(contentType);
    }

    /** Đuôi file lấy từ mime type đã xác thực, dùng khi promote lên key chính thức. */
    public static String extensionOf(String mimeType) {
        if (mimeType == null) {
            return "bin";
        }
        return switch (mimeType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "video/mp4" -> "mp4";
            case "video/quicktime" -> "mov";
            case "video/x-msvideo" -> "avi";
            case "video/webm" -> "webm";
            case "video/x-matroska" -> "mkv";
            case "audio/mpeg" -> "mp3";
            case "audio/wav" -> "wav";
            case "audio/ogg" -> "ogg";
            case "audio/mp4" -> "m4a";
            default -> "bin";
        };
    }

    private static boolean isMp3FrameSync(byte[] header) {
        if (header[0] != MP3_FRAME_SYNC[0]) {
            return false;
        }
        // 11 bit sync (0xFF Ex/Fx) — byte thứ hai có 3 bit cao luôn bằng 1.
        if (header.length < 2) {
            return false;
        }
        int second = header[1] & 0xFF;
        return (second & 0xE0) == 0xE0;
    }

    /**
     * EBML mở đầu bằng DocType ngay sau header. Trong 64 byte đầu, tên doc type nằm ở
     * các offset cố định theo định dạng, nên thử lần lượt các độ dài phổ biến.
     */
    private static String readDocType(byte[] header) {
        for (String candidate : EBML_DOCTYPES.keySet()) {
            byte[] expected = candidate.getBytes(StandardCharsets.US_ASCII);
            if (matchesAt(header, EBML_DOCTYPE_OFFSET, expected)) {
                return candidate;
            }
            if (matchesAt(header, EBML_DOCTYPE_OFFSET + 2, expected)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean startsWith(byte[] header, byte[] prefix) {
        return matchesAt(header, 0, prefix);
    }

    private static boolean matchesAt(byte[] header, int offset, byte[] expected) {
        if (header.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (header[offset + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private static String readAscii(byte[] header, int offset, int length) {
        if (header.length < offset + length) {
            return "";
        }
        return new String(header, offset, length, StandardCharsets.US_ASCII);
    }
}
