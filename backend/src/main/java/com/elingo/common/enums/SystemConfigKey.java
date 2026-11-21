package com.elingo.common.enums;

import com.elingo.common.entity.ConfigModule;
import com.elingo.common.entity.ConfigValueType;
import lombok.Getter;

@Getter
public enum SystemConfigKey {

    // ---- PREMIUM ----
    BANK_CODE_PREFIX("app.premium.bank-transfer.code-prefix", ConfigValueType.STRING, false, ConfigModule.PREMIUM),
    BANK_NAME("app.premium.bank-transfer.bank-name", ConfigValueType.STRING, false, ConfigModule.PREMIUM),
    BANK_ACCOUNT("app.premium.bank-transfer.bank-account", ConfigValueType.STRING, false, ConfigModule.PREMIUM),
    BANK_ACCOUNT_NAME("app.premium.bank-transfer.bank-account-name", ConfigValueType.STRING, false, ConfigModule.PREMIUM),
    BANK_QR_TEMPLATE("app.premium.bank-transfer.qr-template", ConfigValueType.STRING, false, ConfigModule.PREMIUM),
    SEPAY_SECRET_KEY("app.premium.sepay.secret-key", ConfigValueType.STRING, true, ConfigModule.PREMIUM),

    // ---- COMMUNITY ----
    MODERATION_OPENAI_API_KEY("app.community.moderation.openai-api-key", ConfigValueType.STRING, true, ConfigModule.COMMUNITY),
    MODERATION_OPENAI_BASE_URL("app.community.moderation.openai-base-url", ConfigValueType.STRING, false, ConfigModule.COMMUNITY),
    MODERATION_OPENAI_MODEL("app.community.moderation.openai-model", ConfigValueType.STRING, false, ConfigModule.COMMUNITY),

    // ---- GAMIFICATION RULES ----
    GAMIFICATION_RULE_FLASHCARD_REVIEW_XP("app.gamification.rules.flashcard-review.xp", ConfigValueType.INT, false, ConfigModule.GAMIFICATION, "2"),
    GAMIFICATION_RULE_FLASHCARD_REVIEW_DAILY_LIMIT("app.gamification.rules.flashcard-review.daily-limit", ConfigValueType.INT, false, ConfigModule.GAMIFICATION, "100"),

    GAMIFICATION_RULE_SEGMENT_DICTATION_XP("app.gamification.rules.segment-dictation.xp", ConfigValueType.INT, false, ConfigModule.GAMIFICATION, "3"),
    GAMIFICATION_RULE_SEGMENT_DICTATION_DAILY_LIMIT("app.gamification.rules.segment-dictation.daily-limit", ConfigValueType.INT, false, ConfigModule.GAMIFICATION, "50"),

    GAMIFICATION_RULE_SEGMENT_SHADOWING_XP("app.gamification.rules.segment-shadowing.xp", ConfigValueType.INT, false, ConfigModule.GAMIFICATION, "3"),
    GAMIFICATION_RULE_SEGMENT_SHADOWING_DAILY_LIMIT("app.gamification.rules.segment-shadowing.daily-limit", ConfigValueType.INT, false, ConfigModule.GAMIFICATION, "50"),

    GAMIFICATION_RULE_BATTLE_WIN_XP("app.gamification.rules.battle-win.xp", ConfigValueType.INT, false, ConfigModule.GAMIFICATION, "30"),
    GAMIFICATION_RULE_BATTLE_WIN_DAILY_LIMIT("app.gamification.rules.battle-win.daily-limit", ConfigValueType.INT, false, ConfigModule.GAMIFICATION, "10"),

    GAMIFICATION_RULE_BATTLE_LOSE_XP("app.gamification.rules.battle-lose.xp", ConfigValueType.INT, false, ConfigModule.GAMIFICATION, "10"),
    GAMIFICATION_RULE_BATTLE_LOSE_DAILY_LIMIT("app.gamification.rules.battle-lose.daily-limit", ConfigValueType.INT, false, ConfigModule.GAMIFICATION, "10"),

    GAMIFICATION_RULE_DAILY_CHECKIN_XP("app.gamification.rules.daily-checkin.xp", ConfigValueType.INT, false, ConfigModule.GAMIFICATION, "5"),
    GAMIFICATION_RULE_DAILY_CHECKIN_DAILY_LIMIT("app.gamification.rules.daily-checkin.daily-limit", ConfigValueType.INT, false, ConfigModule.GAMIFICATION, "1");

    private final String key;
    private final ConfigValueType type;
    private final boolean secret;
    private final ConfigModule module;
    private final String defaultValue;

    SystemConfigKey(String key, ConfigValueType type, boolean secret, ConfigModule module) {
        this(key, type, secret, module, "");
    }

    SystemConfigKey(String key, ConfigValueType type, boolean secret, ConfigModule module, String defaultValue) {
        this.key = key;
        this.type = type;
        this.secret = secret;
        this.module = module;
        this.defaultValue = defaultValue;
    }
}
