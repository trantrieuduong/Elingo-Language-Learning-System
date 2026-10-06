package com.elingo.common.enums;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Nhóm định dạng của một file, đồng thời là <b>nguồn duy nhất</b> cho danh sách mime type,
 * đuôi file và trần dung lượng được phép upload.
 *
 * <p>Thêm một định dạng mới chỉ cần sửa bảng mime → đuôi file của nhóm tương ứng ở đây;
 * {@code FileSignatureVerifier} và {@code FileKey} đều suy ra từ bảng này. (Riêng chữ ký
 * nhị phân của định dạng mới vẫn phải khai báo trong {@code FileSignatureVerifier.detect}.)
 *
 * <p>Module sở hữu dữ liệu dùng enum này để chặn file sai chỗ trước khi ghi entity: ảnh
 * đại diện chỉ nhận {@link #IMAGE}, bài viết chỉ nhận {@link #IMAGE} và {@link #VIDEO},
 * audio shadowing chỉ nhận {@link #AUDIO}. Kiểm tra phải nằm trong transaction và trước
 * {@code save()}.
 */
public enum MediaKind {

    IMAGE("image",
            Map.of(
                    "image/jpeg", "jpg",
                    "image/png", "png",
                    "image/gif", "gif",
                    "image/webp", "webp"),
            5 * 1024L * 1024),

    VIDEO("video",
            Map.of(
                    "video/mp4", "mp4",
                    "video/quicktime", "mov",
                    "video/x-msvideo", "avi",
                    "video/webm", "webm",
                    "video/x-matroska", "mkv"),
            100 * 1024L * 1024),

    AUDIO("audio",
            Map.of(
                    "audio/mpeg", "mp3",
                    "audio/wav", "wav",
                    "audio/ogg", "ogg",
                    "audio/mp4", "m4a"),
            25 * 1024L * 1024);

    private static final Map<String, MediaKind> BY_EXTENSION;

    private static final Map<String, String> EXTENSION_BY_MIME_TYPE;

    static {
        Map<String, MediaKind> byExtension = new HashMap<>();
        Map<String, String> extensionByMimeType = new HashMap<>();
        for (MediaKind kind : values()) {
            for (Map.Entry<String, String> entry : kind.mimeToExtension.entrySet()) {
                extensionByMimeType.put(entry.getKey(), entry.getValue());
                if (byExtension.put(entry.getValue(), kind) != null) {
                    throw new IllegalStateException("Duplicate extension: " + entry.getValue());
                }
            }
        }
        BY_EXTENSION = Map.copyOf(byExtension);
        EXTENSION_BY_MIME_TYPE = Map.copyOf(extensionByMimeType);
    }

    private final String mimePrefix;

    private final Map<String, String> mimeToExtension;

    private final List<String> extensions;

    private final long maxBytes;

    /**
     * @param mimePrefix      phần loại chính của mime type đứng trước dấu {@code /}, viết thường
     *                        (ví dụ {@code image} cho {@code image/png})
     * @param mimeToExtension mime type chuẩn được phép → đuôi file dùng khi ghi key
     * @param maxBytes        tham số cuối cùng — trần dung lượng của nhóm, tính bằng byte
     */
    MediaKind(String mimePrefix, Map<String, String> mimeToExtension, long maxBytes) {
        this.mimePrefix = mimePrefix;
        this.mimeToExtension = mimeToExtension;
        this.extensions = List.copyOf(mimeToExtension.values());
        this.maxBytes = maxBytes;
    }

    /**
     * Phần loại chính của mime type thuộc nhóm này, ví dụ {@code image}, {@code video},
     * {@code audio}.
     */
    public String mimePrefix() {
        return mimePrefix;
    }

    /** Các mime type chuẩn mà nhóm này cho phép upload. */
    public Set<String> mimeTypes() {
        return mimeToExtension.keySet();
    }

    /** Đuôi file mà nhóm này cho phép. */
    public List<String> extensions() {
        return extensions;
    }

    /**
     * Trần dung lượng của nhóm này, áp cho từng file một.
     *
     * @return số byte tối đa cho một file thuộc nhóm này
     */
    public long maxBytes() {
        return maxBytes;
    }

    /**
     * Mime type này có nằm trong danh sách cho phép của nhóm không (so khớp chính xác, sau
     * khi chuẩn hoá bằng {@link #normalizeMimeType}).
     */
    public boolean supports(String mimeType) {
        String normalized = normalizeMimeType(mimeType);
        return normalized != null && mimeToExtension.containsKey(normalized);
    }

    /**
     * Chuẩn hoá mime type: bỏ phần tham số ({@code ;charset=...}, {@code ;codecs=...}),
     * cắt khoảng trắng và hạ chữ thường.
     *
     * @return mime type chuẩn hoá, hoặc {@code null} nếu đầu vào là {@code null}
     */
    public static String normalizeMimeType(String mimeType) {
        if (mimeType == null) {
            return null;
        }
        int separator = mimeType.indexOf(';');
        String base = separator >= 0 ? mimeType.substring(0, separator) : mimeType;
        return base.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Suy ra nhóm từ đuôi file, không phải từ tên file đầy đủ.
     *
     * @return nhóm tương ứng, hoặc {@code null} nếu đuôi không thuộc nhóm nào
     */
    public static MediaKind fromExtension(String extension) {
        if (extension == null) {
            return null;
        }
        return BY_EXTENSION.get(extension.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Suy ra nhóm từ mime type, chỉ dựa vào phần loại chính trước dấu {@code /}
     * ({@code image/jpg}, {@code image/png} đều ra {@link #IMAGE}).
     *
     * <p>Đây là phép <b>phân nhóm lỏng</b>: {@code image/svg+xml} hay {@code image/bmp}
     * cũng ra {@link #IMAGE}. Muốn biết mime type có được phép upload không, gọi thêm
     * {@link #supports}.
     *
     * @return nhóm tương ứng, hoặc {@code null} nếu mime type rỗng, sai định dạng
     *         hoặc loại chính không thuộc nhóm nào
     */
    public static MediaKind fromMimeType(String mimeType) {
        String normalized = normalizeMimeType(mimeType);
        if (normalized == null) {
            return null;
        }
        int slash = normalized.indexOf('/');
        if (slash <= 0) {
            return null;
        }
        String prefix = normalized.substring(0, slash);
        for (MediaKind kind : values()) {
            if (kind.mimePrefix.equals(prefix)) {
                return kind;
            }
        }
        return null;
    }

    /**
     * Đuôi file ứng với mime type đã xác thực, dùng khi sinh key.
     *
     * @return đuôi file, hoặc {@code null} nếu mime type không nằm trong danh sách cho phép
     */
    public static String extensionOf(String mimeType) {
        String normalized = normalizeMimeType(mimeType);
        return normalized == null ? null : EXTENSION_BY_MIME_TYPE.get(normalized);
    }

    /** Toàn bộ mime type được phép của mọi nhóm. */
    public static Collection<String> allMimeTypes() {
        return EXTENSION_BY_MIME_TYPE.keySet();
    }
}