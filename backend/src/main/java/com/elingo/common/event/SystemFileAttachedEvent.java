package com.elingo.common.event;

/**
 * Một file hệ thống đã được, cần chuyển từ vùng chờ sang vùng
 * lưu trữ vĩnh viễn của hệ thống trên bucket R2:
 * {@code verified/{userId}/{filename}} -&gt; {@code system/{module}/{filename}}.
 *
 * @param verifiedFileKey key file đang nằm ở vùng verified của admin
 * @param systemFileKey   key đích ở vùng hệ thống (dạng {@code system/{module}/{filename}})
 */
public record SystemFileAttachedEvent(String verifiedFileKey, String systemFileKey) {

    public SystemFileAttachedEvent {
        verifiedFileKey = verifiedFileKey == null ? "" : verifiedFileKey;
        systemFileKey = systemFileKey == null ? "" : systemFileKey;
    }
}
