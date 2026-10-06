package com.elingo.file.util;

import com.elingo.common.enums.MediaKind;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Suy ra mime type từ chữ ký nhị phân (magic bytes) của file, không tin vào
 * Content-Type hay đuôi file do client khai.
 *
 * <p>Đọc tối thiểu {@value #SIGNATURE_LENGTH} byte đầu file. Ba định dạng container
 * bắt buộc phải đọc thêm ngoài 4 byte đầu:
 * <ul>
 *   <li><b>RIFF</b> — offset 8 phân biệt WAV, AVI và WebP (cùng chữ ký {@code RIFF}).</li>
 *   <li><b>ISO-BMFF</b> — offset 4 {@code ftyp} rồi đọc brand phân biệt MP4, MOV, M4A.</li>
 *   <li><b>EBML</b> — duyệt các element trong EBML header để tìm element DocType
 *       ({@code 0x4282}), phân biệt WebM và Matroska. Vị trí của DocType phụ thuộc vào
 *       các element đứng trước nó nên không có offset cố định.</li>
 * </ul>
 *
 * <p>Định dạng không khớp bảng trả về {@code null} — coi như không được phép upload.
 * Lưu ý {@code image/svg+xml} không có chữ ký riêng nên không nằm trong bảng này.
 *
 * <p>Danh sách mime type/đuôi file được phép nằm ở {@link MediaKind}; mime type do
 * {@link #detect} trả về luôn phải là một khoá trong bảng đó.
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
    private static final byte[] FTYP = {0x66, 0x74, 0x79, 0x70};

    private static final int RIFF_SUBTYPE_OFFSET = 8;
    private static final int FTYP_OFFSET = 4;
    private static final int FTYP_BRAND_OFFSET = 8;

    /** ID của element DocType trong EBML header (giữ nguyên bit marker). */
    private static final long EBML_DOCTYPE_ID = 0x4282L;
    private static final int EBML_MAX_ID_LENGTH = 4;

    private static final String WAVE_SIGNATURE = "WAVE";
    private static final String AVI_SIGNATURE = "AVI ";
    private static final String WEBP_SIGNATURE = "WEBP";

    private static final Map<String, String> RIFF_SUBTYPES = Map.of(
            WAVE_SIGNATURE, "audio/wav",
            AVI_SIGNATURE, "video/x-msvideo",
            WEBP_SIGNATURE, "image/webp"
    );

    private static final Map<String, String> ISO_BMFF_BRANDS = Map.of(
            "qt  ", "video/quicktime",
            "isom", "video/mp4",
            "iso2", "video/mp4",
            "mp41", "video/mp4",
            "mp42", "video/mp4",
            "avc1", "video/mp4",
            "M4V ", "video/mp4",
            "dash", "video/mp4",
            "M4A ", "audio/mp4",
            "M4B ", "audio/mp4"
    );

    private static final Map<String, String> EBML_DOCTYPES = Map.of(
            "webm", "video/webm",
            "matroska", "video/x-matroska"
    );

    public static final List<String> ALLOWED_IMAGE_TYPES = List.copyOf(MediaKind.IMAGE.mimeTypes());

    public static final List<String> ALLOWED_VIDEO_TYPES = List.copyOf(MediaKind.VIDEO.mimeTypes());

    public static final List<String> ALLOWED_AUDIO_TYPES = List.copyOf(MediaKind.AUDIO.mimeTypes());

    public static final List<String> ALLOWED_FILE_TYPES =
            Stream.of(ALLOWED_IMAGE_TYPES, ALLOWED_VIDEO_TYPES, ALLOWED_AUDIO_TYPES)
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
        return MediaKind.normalizeMimeType(contentType);
    }

    public static boolean isAllowedFileType(String contentType) {
        String normalized = MediaKind.normalizeMimeType(contentType);
        return normalized != null && ALLOWED_FILE_TYPES.contains(normalized);
    }

    private static boolean isMp3FrameSync(byte[] header) {
        if (header.length < 2 || header[0] != (byte) 0xFF) {
            return false;
        }
        // 11 bit sync (0xFF Ex/Fx) — byte thứ hai có 3 bit cao luôn bằng 1.
        return ((header[1] & 0xFF) & 0xE0) == 0xE0;
    }

    /**
     * Duyệt các element trong EBML header để lấy giá trị DocType.
     *
     * <p>Cấu trúc: {@code 1A 45 DF A3} (ID của EBML header) + size (vint) + danh sách
     * element, mỗi element là {@code ID (vint, giữ marker) + size (vint) + dữ liệu}. DocType
     * có ID {@code 0x4282} và nằm sau EBMLVersion, EBMLReadVersion, MaxIDLength,
     * MaxSizeLength nên thường ở khoảng offset 24, không cố định.
     *
     * @return chuỗi doc type ({@code webm}, {@code matroska}...), hoặc {@code null} nếu
     *         không tìm thấy trong phần header đã đọc
     */
    private static String readDocType(byte[] header) {
        int pos = EBML.length;

        int headerSizeLength = vintLength(header, pos);
        if (headerSizeLength < 0) {
            return null;
        }
        long headerSize = vintValue(header, pos, headerSizeLength);
        pos += headerSizeLength;
        long end = Math.min(header.length, pos + headerSize);

        while (pos < end) {
            int idLength = vintLength(header, pos);
            if (idLength < 0 || idLength > EBML_MAX_ID_LENGTH) {
                return null;
            }
            long id = rawValue(header, pos, idLength);
            pos += idLength;

            int sizeLength = vintLength(header, pos);
            if (sizeLength < 0) {
                return null;
            }
            long size = vintValue(header, pos, sizeLength);
            pos += sizeLength;

            if (size < 0 || pos + size > header.length) {
                return null;
            }
            if (id == EBML_DOCTYPE_ID) {
                return new String(header, pos, (int) size, StandardCharsets.US_ASCII)
                        .replace("\0", "")
                        .trim();
            }
            pos += (int) size;
        }
        return null;
    }

    /**
     * Độ dài (byte) của số nguyên EBML bắt đầu tại {@code pos}: số bit 0 đứng đầu byte đầu
     * tiên cộng một.
     *
     * @return độ dài 1..8, hoặc -1 nếu byte đầu bằng 0, vượt hết mảng hoặc không đủ byte
     */
    private static int vintLength(byte[] header, int pos) {
        if (pos >= header.length) {
            return -1;
        }
        int first = header[pos] & 0xFF;
        if (first == 0) {
            return -1;
        }
        int length = Integer.numberOfLeadingZeros(first) - 23;
        return pos + length <= header.length ? length : -1;
    }

    /** Giá trị của vint sau khi bỏ bit marker (dùng cho trường size). */
    private static long vintValue(byte[] header, int pos, int length) {
        long value = header[pos] & (0xFF >> length);
        for (int i = 1; i < length; i++) {
            value = (value << 8) | (header[pos + i] & 0xFF);
        }
        return value;
    }

    /** Giá trị của vint giữ nguyên bit marker (dùng cho trường ID). */
    private static long rawValue(byte[] header, int pos, int length) {
        long value = 0;
        for (int i = 0; i < length; i++) {
            value = (value << 8) | (header[pos + i] & 0xFF);
        }
        return value;
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