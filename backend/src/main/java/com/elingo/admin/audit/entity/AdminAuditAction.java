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
    SYSTEM_DECK_UPDATE,
    USER_DECK_DELETE,
    TOPIC_CREATE,
    TOPIC_UPDATE,
    TOPIC_DELETE,
    CARD_CREATE,
    CARD_UPDATE,
    CARD_DELETE,
    TAG_CREATE,
    TAG_UPDATE,
    TAG_DELETE,
    ORPHANED_CARDS_CLEANUP,
    LESSON_CREATE,
    LESSON_UPDATE,
    LESSON_DELETE,
    LESSON_PUBLISH,
    LESSON_UNPUBLISH,
    LESSON_ARCHIVE,
    SEGMENT_CREATE,
    SEGMENT_UPDATE,
    SEGMENT_DELETE,

    // Cộng đồng
    POST_APPROVE,
    POST_REJECT,
    POST_GROUP_CREATE,
    POST_GROUP_UPDATE,
    POST_GROUP_DELETE,

    // Báo cáo vi phạm
    REPORT_RESOLVE,
    REPORT_REJECT,

    // Premium
    PLAN_CREATE,
    PLAN_UPDATE,
    PLAN_DISABLE,
    PLAN_ENABLE,
    TRANSACTION_MANUAL_CONFIRM,
    TRANSACTION_REFUND,
    SUBSCRIPTION_GRANT,
    SUBSCRIPTION_REVOKE,

    // Cấu hình hệ thống
    SYSTEM_CONFIG_UPDATE,

    // Gamification
    LEVEL_CREATE,
    LEVEL_UPDATE,
    LEVEL_DELETE,
    BADGE_CREATE,
    BADGE_UPDATE,
    BADGE_DELETE
}
