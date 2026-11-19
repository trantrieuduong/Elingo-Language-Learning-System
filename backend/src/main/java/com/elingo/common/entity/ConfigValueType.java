package com.elingo.common.entity;

import java.math.BigDecimal;

public enum ConfigValueType {
    STRING, INT, LONG, BOOLEAN, DECIMAL;

    public void validate(String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String v = value.trim();
        try {
            switch (this) {
                case INT -> Integer.parseInt(v);
                case LONG -> Long.parseLong(v);
                case DECIMAL -> new BigDecimal(v);
                case BOOLEAN -> {
                    if (!"true".equalsIgnoreCase(v) && !"false".equalsIgnoreCase(v)) {
                        throw new IllegalArgumentException("Boolean must be true or false");
                    }
                }
                case STRING -> { /* mọi chuỗi đều hợp lệ */ }
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid format for " + this.name() + ": " + v, e);
        }
    }
}
