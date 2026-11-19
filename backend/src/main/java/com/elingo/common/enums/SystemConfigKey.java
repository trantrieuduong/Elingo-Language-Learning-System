package com.elingo.common.enums;

import com.elingo.common.entity.ConfigModule;
import com.elingo.common.entity.ConfigValueType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
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
    MODERATION_OPENAI_MODEL("app.community.moderation.openai-model", ConfigValueType.STRING, false, ConfigModule.COMMUNITY);

    private final String key;
    private final ConfigValueType type;
    private final boolean secret;
    private final ConfigModule module;
}
