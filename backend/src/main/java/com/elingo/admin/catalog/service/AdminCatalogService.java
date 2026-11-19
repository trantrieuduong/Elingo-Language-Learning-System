package com.elingo.admin.catalog.service;

import com.elingo.admin.catalog.dto.request.UpdateSystemConfigRequest;
import com.elingo.admin.catalog.dto.response.SystemConfigResponse;
import com.elingo.common.entity.ConfigModule;

import java.util.List;

public interface AdminCatalogService {

    List<SystemConfigResponse> getConfigs(ConfigModule module);

    List<String> getModules();

    SystemConfigResponse updateConfig(String configKey, UpdateSystemConfigRequest request);
}
