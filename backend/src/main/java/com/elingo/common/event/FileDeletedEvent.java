package com.elingo.common.event;

import java.util.List;

/**
 * Một hoặc nhiều file của người dùng không còn được tham chiếu nữa và cần xoá khỏi
 * bucket.
 *
 * <p>Module sở hữu dữ liệu phát event này sau khi đã xoá bản ghi chứa chúng — ví dụ
 * {@code community} xoá một bài viết có nhiều ảnh. Vì một bản ghi có thể tham chiếu
 * nhiều file, event mang <b>danh sách</b> key: listener xoá hết trong một lần chạy sau
 * commit, thay vì phát nhiều event và gọi R2 nhiều lần.
 *
 * <p>Module {@code file} nghe event này để xoá các key đó — xoá sau khi transaction
 * commit, xem {@code FileLifecycleListener}.
 *
 * @param fileKeys  các key cần xoá; rỗng hoặc {@code null} thì listener không làm gì
 */
public record FileDeletedEvent(List<String> fileKeys) {

    public FileDeletedEvent {
        fileKeys = fileKeys == null ? List.of() : List.copyOf(fileKeys);
    }
}
