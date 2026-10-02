package com.elingo.common.event;

/**
 * Một file đã được một bản ghi tham chiếu thật sự, nên nó chuyển từ vùng chờ sang vùng
 * vĩnh viễn trên bucket.
 *
 * <p>Module sở hữu dữ liệu phát event này ngay sau khi đã ghi đường dẫn {@code uploads/}
 * vào trường file của entity mình. Lưu ý CSDL lưu key {@code uploads/} chứ không phải key
 * {@code verified/} mà {@code POST /files/uploads} trả về — nhờ vậy {@code uploads/}
 * không bao giờ chứa file nào chưa có bản ghi trỏ tới.
 *
 * <p><b>Phải nghe bằng {@code AFTER_COMMIT.</b> Copy sang {@code uploads/} trước khi
 * commit thì transaction rollback sẽ để lại file mồ côi ở vùng không có quy tắc vòng đời
 * nào dọn — đúng lỗi mà {@code verified/} sinh ra để chặn.
 *
 * @param verifiedFileKey key file đang nằm ở vùng chờ, sẽ được copy rồi xoá
 * @param uploadsFileKey  key đích, cùng tên với {@code verifiedFileKey}; module sở hữu
 *                        đã ghi chính key này vào CSDL
 */
public record FileAttachedEvent(String verifiedFileKey, String uploadsFileKey) {

    public FileAttachedEvent {
        verifiedFileKey = verifiedFileKey == null ? "" : verifiedFileKey;
        uploadsFileKey = uploadsFileKey == null ? "" : uploadsFileKey;
    }
}
