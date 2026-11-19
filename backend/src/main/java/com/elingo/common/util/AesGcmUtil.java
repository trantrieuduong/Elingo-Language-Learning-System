package com.elingo.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public final class AesGcmUtil {

    private static final String PREFIX = "v1:";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public AesGcmUtil(@Value("${app.config-encryption-key}") String rawKey) {
        String trimmed = rawKey != null ? rawKey.trim() : "";
        byte[] raw;
        if (trimmed.length() == 64 && isHex(trimmed)) {
            raw = HexFormat.of().parseHex(trimmed);
        } else {
            raw = Base64.getDecoder().decode(trimmed);
        }

        if (raw.length != 32) {
            throw new IllegalStateException("config-encryption-key must be 32 bytes (Base64 or 64 hex characters). Generate using: openssl rand -base64 32 or openssl rand -hex 32");
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    public String encrypt(String plainText, String aad) {
        if (plainText == null || plainText.isBlank()) {
            return "";
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] out = ByteBuffer.allocate(iv.length + cipherText.length).put(iv).put(cipherText).array();
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Encrypt failed", e);
        }
    }

    public String decrypt(String encrypted, String aad) {
        if (encrypted == null || encrypted.isBlank()) {
            return "";
        }
        if (!encrypted.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Value is not a valid ciphertext");
        }
        try {
            byte[] all = Base64.getDecoder().decode(encrypted.substring(PREFIX.length()));
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
            byte[] plain = cipher.doFinal(all, IV_LENGTH, all.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Decrypt failed", e);
        }
    }

    private static boolean isHex(String s) {
        for (char c : s.toCharArray()) {
            if (Character.digit(c, 16) == -1) {
                return false;
            }
        }
        return true;
    }
}
