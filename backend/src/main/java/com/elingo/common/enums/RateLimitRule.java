package com.elingo.common.enums;

import java.time.Duration;
import java.util.Locale;

public enum RateLimitRule {

    // BR-01: đăng ký
    SIGNUP_IP("signup", Scope.IP, 5, Duration.ofMinutes(10)),

    // BR-02a + BR-02b: đăng nhập
    LOGIN_IP("login", Scope.IP, 10, Duration.ofMinutes(10)),
    LOGIN_USERNAME("login", Scope.USERNAME, 5, Duration.ofMinutes(10)),

    // BR-03: gửi/gửi lại OTP (các endpoint dùng chung bộ đếm "otp-send")
    OTP_SEND_IP("otp-send", Scope.IP, 5, Duration.ofMinutes(10)),
    OTP_SEND_EMAIL("otp-send", Scope.EMAIL, 5, Duration.ofMinutes(10)),

    // BR-04: nhập OTP + đặt mật khẩu / email mới
    OTP_RESET_IP("otp-reset", Scope.IP, 5, Duration.ofMinutes(10)),
    OTP_RESET_EMAIL("otp-reset", Scope.EMAIL, 5, Duration.ofMinutes(10)),

    // BR-05: nhập OTP xác thực tài khoản
    OTP_VERIFY_IP("otp-verify", Scope.IP, 5, Duration.ofMinutes(10)),
    OTP_VERIFY_EMAIL("otp-verify", Scope.EMAIL, 5, Duration.ofMinutes(10));

    public enum Scope {
        IP,
        EMAIL,
        USERNAME
    }

    private final String bucket;
    private final Scope scope;
    private final int max;
    private final Duration window;

    RateLimitRule(String bucket, Scope scope, int max, Duration window) {
        this.bucket = bucket;
        this.scope = scope;
        this.max = max;
        this.window = window;
    }

    public int max() { return max; }
    public Duration window() { return window; }

    /** Key Redis: rate-limit:otp-send:email:abc@x.com */
    public String key(String subject) {
        String normalized = subject == null ? "" : subject.trim().toLowerCase(Locale.ROOT);
        return "rate-limit:" + bucket + ":" + scope.name() + ":" + normalized;
    }
}
