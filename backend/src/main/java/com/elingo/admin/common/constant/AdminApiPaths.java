package com.elingo.admin.common.constant;

public final class AdminApiPaths {

    private AdminApiPaths() {}

    public static final String BASE = "/admin";

    public static final String USERS = BASE + "/users";
    public static final String DECKS = BASE + "/decks";
    public static final String TOPICS = BASE + "/topics";
    public static final String CARDS = BASE + "/cards";
    public static final String LESSONS = BASE + "/lessons";
    public static final String LESSON_SEGMENTS = BASE + "/lesson-segments";
    public static final String TAGS = BASE + "/tags";
    public static final String ORPHANED_CARDS = BASE + "/orphaned-cards";

    public static final String GAMIFICATION_BADGES = BASE + "/gamification/badges";
    public static final String GAMIFICATION_LEVELS = BASE + "/gamification/levels";

    public static final String SYSTEM_CONFIGS = BASE + "/system-configs";
    public static final String AUDIT_LOGS = BASE + "/audit-logs";

    public static final String STATS_USERS = BASE + "/stats/users";
    public static final String STATS_REVENUE = BASE + "/stats/revenue";
    public static final String STATS_LEARNING = BASE + "/stats/learning";
    public static final String STATS_COMMUNITY = BASE + "/stats/community";

    public static final String PREMIUM_PLANS = BASE + "/premium/plans";
    public static final String PREMIUM_TRANSACTIONS = BASE + "/premium/transactions";
    public static final String PREMIUM_WEBHOOK_LOGS = BASE + "/premium/webhook-logs";
}
