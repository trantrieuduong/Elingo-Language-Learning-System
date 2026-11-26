package com.elingo.common.service.impl;

import com.elingo.common.entity.ConfigModule;
import com.elingo.common.entity.SystemConfig;
import com.elingo.common.enums.SystemConfigKey;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.repository.SystemConfigRepository;
import com.elingo.common.service.SystemConfigService;
import com.elingo.common.util.AesGcmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j(topic = "SYSTEM-CONFIG-SERVICE")
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final String MASKED_SECRET = "********";

    private final SystemConfigRepository repository;
    private final AesGcmUtil aesGcmUtil;

    @SuppressWarnings("java:S3077") // Map bất biến (Map.of/Map.copyOf) chỉ gán lại reference khi reload()
    private volatile Map<String, String> cache = Map.of();

    // -----Startup-----
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        seedMissing();
        reload();
    }

    @Override
    public String getString(SystemConfigKey key, String defaultValue) {
        String val = getString(key);
        return (val == null || val.isBlank()) ? defaultValue : val;
    }

    @Override
    public String getString(SystemConfigKey key) {
        return cache.getOrDefault(key.getKey(), "");
    }

    @Override
    public int getInt(SystemConfigKey key, int defaultValue) {
        String val = getString(key);
        if (val == null || val.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid integer config for key={}: '{}'. Fallback to default={}", key.getKey(), val, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public int getInt(SystemConfigKey key) {
        return Integer.parseInt(required(key).trim());
    }

    @Override
    public boolean getBoolean(SystemConfigKey key, boolean defaultValue) {
        String val = getString(key);
        if (val == null || val.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(val.trim());
    }

    @Override
    public boolean getBoolean(SystemConfigKey key) {
        return Boolean.parseBoolean(required(key).trim());
    }

    @Override
    public void reload() {
        Map<String, String> fresh = new HashMap<>();
        for (SystemConfig c : repository.findAll()) {
            try {
                fresh.put(c.getConfigKey(), decodeIfIsSecret(c));
            } catch (RuntimeException e) {
                log.error("Cannot decode system config key={}", c.getConfigKey(), e);
            }
        }
        cache = Map.copyOf(fresh);
        log.info("System config cache loaded, size={}", fresh.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemConfig> getAll(ConfigModule module) {
        if (module != null) {
            return repository.findAllByModuleOrderByConfigKey(module);
        }
        return repository.findAllByOrderByModuleAscConfigKeyAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public SystemConfig getByKey(String configKey) {
        return repository.findByConfigKey(configKey)
                .orElseThrow(() -> new AppException(AppError.SYSTEM_CONFIG_NOT_FOUND));
    }

    @Override
    @Transactional
    public SystemConfig update(String configKey, String newValue, String description, Long adminId) {
        SystemConfig config = repository.findByConfigKey(configKey)
                .orElseThrow(() -> new AppException(AppError.SYSTEM_CONFIG_NOT_FOUND));

        try {
            config.getValueType().validate(newValue);
        } catch (IllegalArgumentException e) {
            log.error("Validation failed for configKey={} value={}: {}", configKey, newValue, e.getMessage());
            throw new AppException(AppError.SYSTEM_CONFIG_VALUE_INVALID);
        }

        String toStore;
        if (Boolean.TRUE.equals(config.getIsSecret())) {
            if (newValue == null || newValue.isBlank()) {
                toStore = "";
            } else if (MASKED_SECRET.equals(newValue.trim())) {
                toStore = config.getConfigValue(); // giữ nguyên giá trị ciphertext cũ nếu client gửi masked
            } else {
                toStore = aesGcmUtil.encrypt(newValue.trim(), configKey);
            }
        } else {
            toStore = (newValue != null) ? newValue.trim() : "";
        }

        config.setConfigValue(toStore);
        config.setDescription(description);
        config.setUpdatedBy(adminId);

        SystemConfig saved = repository.save(config);
        reload();
        log.info("Updated system config key={} by adminId={}", configKey, adminId);
        return saved;
    }

    private void seedMissing() {
        Set<String> existing = repository.findAll().stream()
                .map(SystemConfig::getConfigKey)
                .collect(Collectors.toSet());

        for (SystemConfigKey def : SystemConfigKey.values()) {
            if (existing.contains(def.getKey())) {
                continue;
            }

            SystemConfig entity = SystemConfig.builder()
                    .configKey(def.getKey())
                    .configValue(def.getDefaultValue() != null ? def.getDefaultValue() : "")
                    .valueType(def.getType())
                    .isSecret(def.isSecret())
                    .module(def.getModule())
                    .description(def.getDescription())
                    .build();
            repository.save(entity);
            log.info("Seeded system config key={}", def.getKey());
        }
    }

    private String decodeIfIsSecret(SystemConfig c) {
        String stored = c.getConfigValue();
        if (stored == null || stored.isBlank()) {
            return "";
        }
        return Boolean.TRUE.equals(c.getIsSecret())
                ? aesGcmUtil.decrypt(stored, c.getConfigKey())
                : stored;
    }

    private String required(SystemConfigKey key) {
        String value = getString(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required system config: " + key.getKey());
        }
        return value;
    }
}
