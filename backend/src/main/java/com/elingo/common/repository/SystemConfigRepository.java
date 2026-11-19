package com.elingo.common.repository;

import com.elingo.common.entity.ConfigModule;
import com.elingo.common.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {

    Optional<SystemConfig> findByConfigKey(String configKey);

    List<SystemConfig> findAllByModuleOrderByConfigKey(ConfigModule module);

    List<SystemConfig> findAllByOrderByModuleAscConfigKeyAsc();
}
