package com.elingo.common.enums;

import com.elingo.common.entity.ConfigModule;
import com.elingo.common.entity.ConfigValueType;
import lombok.Getter;

@Getter
public enum SystemConfigKey {

    // ---- PREMIUM ----
    BANK_CODE_PREFIX("app.premium.bank-transfer.code-prefix", ConfigValueType.STRING, false, ConfigModule.PREMIUM, "", "Prefix code for bank transfer transactions"),
    BANK_NAME("app.premium.bank-transfer.bank-name", ConfigValueType.STRING, false, ConfigModule.PREMIUM, "", "Bank name for payment transfers"),
    BANK_ACCOUNT("app.premium.bank-transfer.bank-account", ConfigValueType.STRING, false, ConfigModule.PREMIUM, "", "Bank account number for payment transfers"),
    BANK_ACCOUNT_NAME("app.premium.bank-transfer.bank-account-name", ConfigValueType.STRING, false, ConfigModule.PREMIUM, "", "Account holder name for payment transfers"),
    BANK_QR_TEMPLATE("app.premium.bank-transfer.qr-template", ConfigValueType.STRING, false, ConfigModule.PREMIUM, "", "VietQR template style"),
    SEPAY_SECRET_KEY("app.premium.sepay.secret-key", ConfigValueType.STRING, true, ConfigModule.PREMIUM, "", "SePay webhook secret authentication key"),
    REFUND_MAX_DAYS("app.premium.refund.max-days", ConfigValueType.INT, false, ConfigModule.PREMIUM, "3", "Maximum number of days after payment within which a transaction can be refunded"),

    // ---- COMMUNITY ----
    MODERATION_OPENAI_API_KEY("app.community.moderation.openai-api-key", ConfigValueType.STRING, true, ConfigModule.COMMUNITY, "", "OpenAI API key for automated content moderation"),
    MODERATION_OPENAI_BASE_URL("app.community.moderation.openai-base-url", ConfigValueType.STRING, false, ConfigModule.COMMUNITY, "", "Base URL for OpenAI moderation API"),
    MODERATION_OPENAI_MODEL("app.community.moderation.openai-model", ConfigValueType.STRING, false, ConfigModule.COMMUNITY, "", "OpenAI model name used for moderation"),

    // ---- GAMIFICATION RULES ----
    GAMIFICATION_RULE_FLASHCARD_REVIEW_XP("app.gamification.rules.flashcard-review.xp", ConfigValueType.INT, false, ConfigModule.GAMIFICATION, "2", "XP points awarded per flashcard review session"),
    GAMIFICATION_RULE_FLASHCARD_REVIEW_DAILY_LIMIT("app.gamification.rules.flashcard-review.daily-limit", ConfigValueType.INT, false, ConfigModule.GAMIFICATION, "100", "Maximum daily XP limit for flashcard reviews"),

    GAMIFICATION_RULE_SEGMENT_DICTATION_XP("app.gamification.rules.segment-dictation.xp", ConfigValueType.INT, false, ConfigModule.GAMIFICATION, "3", "XP points awarded per video dictation segment"),
    GAMIFICATION_RULE_SEGMENT_DICTATION_DAILY_LIMIT("app.gamification.rules.segment-dictation.daily-limit", ConfigValueType.INT, false, ConfigModule.GAMIFICATION, "50", "Maximum daily XP limit for video dictation segments"),

    GAMIFICATION_RULE_SEGMENT_SHADOWING_XP("app.gamification.rules.segment-shadowing.xp", ConfigValueType.INT, false, ConfigModule.GAMIFICATION, "3", "XP points awarded per video shadowing segment"),
    GAMIFICATION_RULE_SEGMENT_SHADOWING_DAILY_LIMIT("app.gamification.rules.segment-shadowing.daily-limit", ConfigValueType.INT, false, ConfigModule.GAMIFICATION, "50", "Maximum daily XP limit for video shadowing segments"),

    GAMIFICATION_RULE_BATTLE_WIN_XP("app.gamification.rules.battle-win.xp", ConfigValueType.INT, false, ConfigModule.GAMIFICATION, "30", "XP points awarded for winning a vocabulary battle"),
    GAMIFICATION_RULE_BATTLE_WIN_DAILY_LIMIT("app.gamification.rules.battle-win.daily-limit", ConfigValueType.INT, false, ConfigModule.GAMIFICATION, "10", "Maximum daily battle wins rewarded with XP"),

    GAMIFICATION_RULE_BATTLE_LOSE_XP("app.gamification.rules.battle-lose.xp", ConfigValueType.INT, false, ConfigModule.GAMIFICATION, "10", "XP points awarded for participating in a lost vocabulary battle"),
    GAMIFICATION_RULE_BATTLE_LOSE_DAILY_LIMIT("app.gamification.rules.battle-lose.daily-limit", ConfigValueType.INT, false, ConfigModule.GAMIFICATION, "10", "Maximum daily battle losses rewarded with XP"),

    GAMIFICATION_RULE_DAILY_CHECKIN_XP("app.gamification.rules.daily-checkin.xp", ConfigValueType.INT, false, ConfigModule.GAMIFICATION, "5", "XP points awarded for daily check-in"),
    GAMIFICATION_RULE_DAILY_CHECKIN_DAILY_LIMIT("app.gamification.rules.daily-checkin.daily-limit", ConfigValueType.INT, false, ConfigModule.GAMIFICATION, "1", "Maximum daily check-in rewards allowed");

    private final String key;
    private final ConfigValueType type;
    private final boolean secret;
    private final ConfigModule module;
    private final String defaultValue;
    private final String description;

    SystemConfigKey(String key, ConfigValueType type, boolean secret, ConfigModule module, String defaultValue, String description) {
        this.key = key;
        this.type = type;
        this.secret = secret;
        this.module = module;
        this.defaultValue = defaultValue;
        this.description = description;
    }
}
