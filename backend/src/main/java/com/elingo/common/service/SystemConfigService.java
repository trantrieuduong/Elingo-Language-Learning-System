package com.elingo.common.service;

import com.elingo.common.entity.ConfigModule;
import com.elingo.common.entity.SystemConfig;
import com.elingo.common.enums.SystemConfigKey;

import java.util.List;

public interface SystemConfigService {

    String getString(SystemConfigKey key, String defaultValue);

    String getString(SystemConfigKey key);

    int getInt(SystemConfigKey key, int defaultValue);

    int getInt(SystemConfigKey key);

    boolean getBoolean(SystemConfigKey key, boolean defaultValue);

    boolean getBoolean(SystemConfigKey key);

    void reload();

    List<SystemConfig> getAll(ConfigModule module);

    SystemConfig getByKey(String configKey);

    SystemConfig update(String configKey, String newValue, String description, Long adminId);
}
