package com.elingo.admin.audit.entity;

/**
 * Thao tác quản trị được ghi lại. Lưu dạng STRING trong DB,
 * nên có thể thêm giá trị mới mà không ảnh hưởng dữ liệu cũ.
 * Không đổi tên giá trị đã dùng (sẽ lệch với log cũ).
 */
public enum AdminAuditAction {
    // Người dùng
    USER_LOCK,
    USER_UNLOCK,
    USER_ROLE_CHANGE,

    // Nội dung học tập
    SYSTEM_DECK_PUBLISH,
    SYSTEM_DECK_ARCHIVE,
    SYSTEM_DECK_CREATE,
    USER_DECK_DELETE,
    LESSON_CREATE,
    LESSON_UPDATE,
    LESSON_DELETE,
    SEGMENT_CREATE,
    SEGMENT_UPDATE,
    SEGMENT_DELETE,

    // Kiểm duyệt cộng đồng
    POST_APPROVE,
    POST_REJECT,

    // Báo cáo vi phạm
    REPORT_RESOLVE,
    REPORT_REJECT,

    // Premium
    PLAN_CREATE,
    PLAN_UPDATE,
    PLAN_DISABLE,
    SUBSCRIPTION_GRANT,
    SUBSCRIPTION_REVOKE,
}
