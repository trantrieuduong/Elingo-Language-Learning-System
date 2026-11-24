package com.elingo.admin.systemconfig.service;

import com.elingo.admin.systemconfig.dto.request.UpdateSystemConfigRequest;
import com.elingo.admin.systemconfig.dto.response.SystemConfigResponse;
import com.elingo.common.entity.ConfigModule;

import java.util.List;

public interface AdminSystemConfigService {

    List<SystemConfigResponse> getConfigs(ConfigModule module);

    List<String> getModules();

    SystemConfigResponse updateConfig(String configKey, UpdateSystemConfigRequest request, Long adminId);
}
