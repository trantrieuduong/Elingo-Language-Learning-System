package com.elingo.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@AllArgsConstructor
@Getter
public enum AppError {

    // ==========================================
    // 1. Common & Validation (Hệ thống & Dữ liệu chung)
    // ==========================================
    UNCATEGORIZED_EXCEPTION("UNCATEGORIZED_EXCEPTION", "Uncategorized exception", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST("INVALID_REQUEST", "Invalid request parameters or body", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "Requested resource not found", HttpStatus.NOT_FOUND),
    METHOD_NOT_SUPPORTED("METHOD_NOT_SUPPORTED", "HTTP request method is not supported", HttpStatus.METHOD_NOT_ALLOWED),
    MEDIA_TYPE_NOT_SUPPORTED("MEDIA_TYPE_NOT_SUPPORTED", "Request media type is not supported", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    MEDIA_TYPE_NOT_ACCEPTABLE("MEDIA_TYPE_NOT_ACCEPTABLE", "Requested media type cannot be served", HttpStatus.NOT_ACCEPTABLE),
    MALFORMED_JSON("MALFORMED_JSON", "Malformed JSON request or invalid data format", HttpStatus.BAD_REQUEST),
    PARAM_MISSING("PARAM_MISSING", "Required request parameter is missing", HttpStatus.BAD_REQUEST),
    PARAM_TYPE_MISMATCH("PARAM_TYPE_MISMATCH", "Parameter type mismatch", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "Internal server error occurred", HttpStatus.INTERNAL_SERVER_ERROR),
    EXTERNAL_SERVICE_ERROR("EXTERNAL_SERVICE_ERROR", "Failed to communicate with external service", HttpStatus.BAD_GATEWAY),
    FILE_UPLOAD_FAILED("FILE_UPLOAD_FAILED", "Failed to upload file", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_TOO_LARGE("FILE_TOO_LARGE", "Uploaded file size exceeds the allowed limit", HttpStatus.BAD_REQUEST),
    INVALID_FILE_TYPE("INVALID_FILE_TYPE", "File format is not supported", HttpStatus.BAD_REQUEST),

    // ==========================================
    // 2. Auth & User (Xác thực & Người dùng)
    // ==========================================
    USERNAME_INVALID("USERNAME_INVALID", "Username must be 3-15 characters, letters and numbers only, no spaces", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID("PASSWORD_INVALID", "Password must be at least 8 characters and include uppercase, lowercase, number, and special character", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID("EMAIL_INVALID", "Email format is invalid", HttpStatus.BAD_REQUEST),
    FULL_NAME_INVALID("FULL_NAME_INVALID", "Full name must not be blank and less than 150 characters", HttpStatus.BAD_REQUEST),
    OTP_INVALID("OTP_INVALID", "OTP is incorrect or expired", HttpStatus.BAD_REQUEST),
    USERNAME_EXISTED("USERNAME_EXISTED", "Username already exists", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTED("EMAIL_EXISTED", "Email already exists", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_EXISTED("EMAIL_NOT_EXISTED", "Email doesn't exist", HttpStatus.NOT_FOUND),
    EMAIL_UNCHANGED("EMAIL_UNCHANGED", "New email and old email are the same", HttpStatus.BAD_REQUEST),
    EMAIL_SEND_FAILED("EMAIL_SEND_FAILED", "Failed to send email", HttpStatus.INTERNAL_SERVER_ERROR),
    UNAUTHENTICATED("UNAUTHENTICATED", "Incorrect or expired authentication token", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED("UNAUTHORIZED", "You do not have permission to access this resource", HttpStatus.FORBIDDEN),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "Incorrect email, username or password", HttpStatus.UNAUTHORIZED),
    USER_NOT_FOUND("USER_NOT_FOUND", "User not found", HttpStatus.NOT_FOUND),
    USER_INACTIVE("USER_INACTIVE", "User account is currently inactive", HttpStatus.FORBIDDEN),
    USER_BANNED("USER_BANNED", "User account has been banned", HttpStatus.FORBIDDEN),
    USER_NOT_VERIFIED("USER_NOT_VERIFIED", "Email has not been verified yet", HttpStatus.FORBIDDEN),
    REFRESH_TOKEN_INVALID("REFRESH_TOKEN_INVALID", "Refresh token token is missing, invalid or expired", HttpStatus.UNAUTHORIZED),
    PASSWORD_INCORRECT("PASSWORD_INCORRECT", "Password is incorrect", HttpStatus.BAD_REQUEST),
    OLD_PASSWORD_INCORRECT("OLD_PASSWORD_INCORRECT", "Old password is incorrect", HttpStatus.BAD_REQUEST),
    NEW_PASSWORD_SAME_AS_OLD("NEW_PASSWORD_SAME_AS_OLD", "New password must be different from old password", HttpStatus.BAD_REQUEST),
    CANNOT_CHANGE_USERNAME_YET("CANNOT_CHANGE_USERNAME_YET", "Username can only be changed once within allowed cooldown period", HttpStatus.BAD_REQUEST),
    GOOGLE_TOKEN_INVALID("GOOGLE_TOKEN_INVALID", "Google authentication token is invalid or expired", HttpStatus.BAD_REQUEST),
    GOOGLE_TOKEN_VERIFICATION_FAILED("GOOGLE_TOKEN_VERIFICATION_FAILED", "Failed to verify Google token", HttpStatus.INTERNAL_SERVER_ERROR),
    PASSWORD_ALREADY_SET("PASSWORD_ALREADY_SET", "Password is already set for this account, use change password instead", HttpStatus.BAD_REQUEST),

    // ==========================================
    // 3. Vocabulary (Bộ từ vựng, Chủ đề, Thẻ từ)
    // ==========================================
    DECK_NOT_FOUND("DECK_NOT_FOUND", "Vocabulary deck not found", HttpStatus.NOT_FOUND),
    DECK_SLUG_EXISTED("DECK_SLUG_EXISTED", "Deck slug already exists", HttpStatus.BAD_REQUEST),
    DECK_ACCESS_DENIED("DECK_ACCESS_DENIED", "You do not have permission to modify this deck", HttpStatus.FORBIDDEN),
    DECK_PREMIUM_REQUIRED("DECK_PREMIUM_REQUIRED", "This deck requires a premium subscription", HttpStatus.FORBIDDEN),
    DECK_EMPTY("DECK_EMPTY", "Deck contains no cards", HttpStatus.BAD_REQUEST),
    TOPIC_NOT_FOUND("TOPIC_NOT_FOUND", "Topic not found", HttpStatus.NOT_FOUND),
    TOPIC_SLUG_EXISTED("TOPIC_SLUG_EXISTED", "Topic slug already exists in this deck", HttpStatus.BAD_REQUEST),
    CARD_NOT_FOUND("CARD_NOT_FOUND", "Vocabulary card not found", HttpStatus.NOT_FOUND),
    CARD_PHONETIC_NOT_FOUND("CARD_PHONETIC_NOT_FOUND", "Card phonetic not found", HttpStatus.NOT_FOUND),
    TAG_NOT_FOUND("TAG_NOT_FOUND", "Tag not found", HttpStatus.NOT_FOUND),
    TAG_CODE_EXISTED("TAG_CODE_EXISTED", "Tag code already exists", HttpStatus.BAD_REQUEST),
    CEFR_LEVEL_NOT_FOUND("CEFR_LEVEL_NOT_FOUND", "CEFR level not found", HttpStatus.NOT_FOUND),
    CEFR_LEVEL_CODE_EXISTED("CEFR_LEVEL_CODE_EXISTED", "CEFR level code already exists", HttpStatus.BAD_REQUEST),
    IMPORT_VOCABULARY_FAILED("IMPORT_VOCABULARY_FAILED", "Failed to import vocabulary from file", HttpStatus.BAD_REQUEST),

    // ==========================================
    // 4. Learning - Flashcard (Học thẻ SRS SM-2)
    // ==========================================
    USER_CARD_STATE_NOT_FOUND("USER_CARD_STATE_NOT_FOUND", "User card study state not found", HttpStatus.NOT_FOUND),
    INVALID_SRS_GRADE("INVALID_SRS_GRADE", "Review grade must be between 0 (Again) and 3 (Easy)", HttpStatus.BAD_REQUEST),
    NO_CARDS_DUE("NO_CARDS_DUE", "No flashcards are due for review", HttpStatus.BAD_REQUEST),

    // ==========================================
    // 5. Learning - Lesson (Bài học, Dictation, Shadowing)
    // ==========================================
    LESSON_NOT_FOUND("LESSON_NOT_FOUND", "Lesson not found", HttpStatus.NOT_FOUND),
    LESSON_SLUG_EXISTED("LESSON_SLUG_EXISTED", "Lesson slug already exists", HttpStatus.BAD_REQUEST),
    LESSON_PREMIUM_REQUIRED("LESSON_PREMIUM_REQUIRED", "This lesson requires a premium subscription", HttpStatus.FORBIDDEN),
    LESSON_SEGMENT_NOT_FOUND("LESSON_SEGMENT_NOT_FOUND", "Lesson segment not found", HttpStatus.NOT_FOUND),
    INVALID_SEGMENT_TIME_RANGE("INVALID_SEGMENT_TIME_RANGE", "Segment start time must be less than end time", HttpStatus.BAD_REQUEST),
    USER_LESSON_PROGRESS_NOT_FOUND("USER_LESSON_PROGRESS_NOT_FOUND", "User lesson progress not found", HttpStatus.NOT_FOUND),
    USER_SEGMENT_PROGRESS_NOT_FOUND("USER_SEGMENT_PROGRESS_NOT_FOUND", "User segment progress not found", HttpStatus.NOT_FOUND),
    AUDIO_RECORDING_REQUIRED("AUDIO_RECORDING_REQUIRED", "Shadowing requires an audio recording file", HttpStatus.BAD_REQUEST),

    // ==========================================
    // 6. Battle (Đấu trường từ vựng 1vs1)
    // ==========================================
    BATTLE_MATCH_NOT_FOUND("BATTLE_MATCH_NOT_FOUND", "Battle match not found", HttpStatus.NOT_FOUND),
    BATTLE_ROOM_NOT_FOUND("BATTLE_ROOM_NOT_FOUND", "Battle room code not found", HttpStatus.NOT_FOUND),
    BATTLE_ROOM_FULL("BATTLE_ROOM_FULL", "Battle room has already reached maximum participants", HttpStatus.BAD_REQUEST),
    BATTLE_ROOM_ALREADY_STARTED("BATTLE_ROOM_ALREADY_STARTED", "Battle match has already started", HttpStatus.BAD_REQUEST),
    BATTLE_MATCH_FINISHED("BATTLE_MATCH_FINISHED", "Battle match has already finished", HttpStatus.BAD_REQUEST),
    BATTLE_MATCH_CANCELLED("BATTLE_MATCH_CANCELLED", "Battle match has been cancelled", HttpStatus.BAD_REQUEST),
    BATTLE_PARTICIPANT_NOT_FOUND("BATTLE_PARTICIPANT_NOT_FOUND", "Battle participant not found in this match", HttpStatus.NOT_FOUND),
    BATTLE_ALREADY_PARTICIPATED("BATTLE_ALREADY_PARTICIPATED", "User has already joined this battle match", HttpStatus.BAD_REQUEST),
    BATTLE_QUESTION_NOT_FOUND("BATTLE_QUESTION_NOT_FOUND", "Battle question not found", HttpStatus.NOT_FOUND),
    BATTLE_QUESTION_TIMEOUT("BATTLE_QUESTION_TIMEOUT", "Answer submitted after question time limit expired", HttpStatus.BAD_REQUEST),
    BATTLE_QUESTION_ALREADY_ANSWERED("BATTLE_QUESTION_ALREADY_ANSWERED", "You have already answered this question", HttpStatus.BAD_REQUEST),
    CANNOT_BATTLE_YOURSELF("CANNOT_BATTLE_YOURSELF", "Cannot start a battle match against yourself", HttpStatus.BAD_REQUEST),

    // ==========================================
    // 7. Speaking Room (Phòng luyện nói)
    // ==========================================
    SPEAKING_ROOM_NOT_FOUND("SPEAKING_ROOM_NOT_FOUND", "Speaking room not found", HttpStatus.NOT_FOUND),
    SPEAKING_ROOM_FULL("SPEAKING_ROOM_FULL", "Speaking room has reached maximum participants", HttpStatus.BAD_REQUEST),
    SPEAKING_ROOM_CLOSED("SPEAKING_ROOM_CLOSED", "Speaking room is already closed", HttpStatus.BAD_REQUEST),
    SPEAKING_ROOM_ACCESS_DENIED("SPEAKING_ROOM_ACCESS_DENIED", "You do not have access to this speaking room", HttpStatus.FORBIDDEN),
    SPEAKING_PARTICIPANT_NOT_FOUND("SPEAKING_PARTICIPANT_NOT_FOUND", "Participant not found in speaking room", HttpStatus.NOT_FOUND),
    HOST_CANNOT_LEAVE_WITHOUT_CLOSING("HOST_CANNOT_LEAVE_WITHOUT_CLOSING", "Host must end the room or transfer host role before leaving", HttpStatus.BAD_REQUEST),

    // ==========================================
    // 8. Gamification (XP, Level, Streak, Check-in, Badge, Leaderboard)
    // ==========================================
    ALREADY_CHECKED_IN_TODAY("ALREADY_CHECKED_IN_TODAY", "You have already checked in today", HttpStatus.BAD_REQUEST),
    LEVEL_NOT_FOUND("LEVEL_NOT_FOUND", "Level configuration not found", HttpStatus.NOT_FOUND),
    USER_XP_NOT_FOUND("USER_XP_NOT_FOUND", "User XP profile not found", HttpStatus.NOT_FOUND),
    BADGE_NOT_FOUND("BADGE_NOT_FOUND", "Badge not found", HttpStatus.NOT_FOUND),
    BADGE_CODE_EXISTED("BADGE_CODE_EXISTED", "Badge code already exists", HttpStatus.BAD_REQUEST),
    BADGE_ALREADY_EARNED("BADGE_ALREADY_EARNED", "User has already earned this badge", HttpStatus.BAD_REQUEST),
    LEADERBOARD_SNAPSHOT_NOT_FOUND("LEADERBOARD_SNAPSHOT_NOT_FOUND", "Leaderboard snapshot not found", HttpStatus.NOT_FOUND),
    INVALID_XP_AMOUNT("INVALID_XP_AMOUNT", "XP adjustment amount must not be zero", HttpStatus.BAD_REQUEST),

    // ==========================================
    // 9. Community (Bài viết, Bình luận, Tương tác)
    // ==========================================
    POST_GROUP_NOT_FOUND("POST_GROUP_NOT_FOUND", "Post group not found", HttpStatus.NOT_FOUND),
    POST_NOT_FOUND("POST_NOT_FOUND", "Post not found", HttpStatus.NOT_FOUND),
    POST_ACCESS_DENIED("POST_ACCESS_DENIED", "You do not have permission to modify or delete this post", HttpStatus.FORBIDDEN),
    POST_NOT_PUBLISHED("POST_NOT_PUBLISHED", "Post is currently not published or has been hidden", HttpStatus.BAD_REQUEST),
    POST_ALREADY_MODERATED("POST_ALREADY_MODERATED", "Post moderation has already been finalized", HttpStatus.BAD_REQUEST),
    COMMENT_NOT_FOUND("COMMENT_NOT_FOUND", "Comment not found", HttpStatus.NOT_FOUND),
    COMMENT_ACCESS_DENIED("COMMENT_ACCESS_DENIED", "You do not have permission to modify or delete this comment", HttpStatus.FORBIDDEN),
    PARENT_COMMENT_NOT_FOUND("PARENT_COMMENT_NOT_FOUND", "Parent comment not found", HttpStatus.NOT_FOUND),
    COMMENT_NESTING_LEVEL_EXCEEDED("COMMENT_NESTING_LEVEL_EXCEEDED", "Comments only support 1 level of nested replies", HttpStatus.BAD_REQUEST),
    POST_ALREADY_LIKED("POST_ALREADY_LIKED", "You have already liked this post", HttpStatus.BAD_REQUEST),
    POST_NOT_LIKED("POST_NOT_LIKED", "You have not liked this post yet", HttpStatus.BAD_REQUEST),
    COMMENT_ALREADY_LIKED("COMMENT_ALREADY_LIKED", "You have already liked this comment", HttpStatus.BAD_REQUEST),
    COMMENT_NOT_LIKED("COMMENT_NOT_LIKED", "You have not liked this comment yet", HttpStatus.BAD_REQUEST),

    // ==========================================
    // 10. Report (Báo cáo vi phạm)
    // ==========================================
    REPORT_NOT_FOUND("REPORT_NOT_FOUND", "Report not found", HttpStatus.NOT_FOUND),
    REPORT_ALREADY_RESOLVED("REPORT_ALREADY_RESOLVED", "Report has already been resolved or rejected", HttpStatus.BAD_REQUEST),
    CANNOT_REPORT_SELF("CANNOT_REPORT_SELF", "You cannot report your own account or content", HttpStatus.BAD_REQUEST),
    REPORT_TARGET_NOT_FOUND("REPORT_TARGET_NOT_FOUND", "The target entity being reported does not exist", HttpStatus.NOT_FOUND),

    // ==========================================
    // 11. Premium & Payment (Gói Premium & Thanh toán)
    // ==========================================
    PREMIUM_PLAN_NOT_FOUND("PREMIUM_PLAN_NOT_FOUND", "Premium plan not found", HttpStatus.NOT_FOUND),
    PREMIUM_PLAN_INACTIVE("PREMIUM_PLAN_INACTIVE", "This premium plan is currently inactive", HttpStatus.BAD_REQUEST),
    PAYMENT_PROVIDER_NOT_FOUND("PAYMENT_PROVIDER_NOT_FOUND", "Payment provider not found", HttpStatus.NOT_FOUND),
    PAYMENT_PROVIDER_INACTIVE("PAYMENT_PROVIDER_INACTIVE", "This payment provider is currently inactive", HttpStatus.BAD_REQUEST),
    PAYMENT_TRANSACTION_NOT_FOUND("PAYMENT_TRANSACTION_NOT_FOUND", "Payment transaction not found", HttpStatus.NOT_FOUND),
    PAYMENT_TRANSACTION_ALREADY_PROCESSED("PAYMENT_TRANSACTION_ALREADY_PROCESSED", "Payment transaction has already been completed or processed", HttpStatus.BAD_REQUEST),
    INVALID_PAYMENT_SIGNATURE("INVALID_PAYMENT_SIGNATURE", "Invalid payment checksum or webhook signature", HttpStatus.BAD_REQUEST),
    PAYMENT_AMOUNT_MISMATCH("PAYMENT_AMOUNT_MISMATCH", "Paid amount does not match transaction plan price", HttpStatus.BAD_REQUEST),
    USER_SUBSCRIPTION_NOT_FOUND("USER_SUBSCRIPTION_NOT_FOUND", "User subscription not found", HttpStatus.NOT_FOUND),
    ACTIVE_SUBSCRIPTION_ALREADY_EXISTS("ACTIVE_SUBSCRIPTION_ALREADY_EXISTS", "User already has an active premium subscription", HttpStatus.BAD_REQUEST),

    // ==========================================
    // 12. Notification (Thông báo)
    // ==========================================
    NOTIFICATION_NOT_FOUND("NOTIFICATION_NOT_FOUND", "Notification not found", HttpStatus.NOT_FOUND),
    NOTIFICATION_ACCESS_DENIED("NOTIFICATION_ACCESS_DENIED", "You do not have permission to access this notification", HttpStatus.FORBIDDEN),
    ;

    private final String code;
    private final String message;
    private final HttpStatusCode httpStatusCode;
}
