package com.elingo.admin.audit.repository;

import com.elingo.admin.audit.entity.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AdminAuditRepository extends JpaRepository<AdminAuditLog, Long>,
        JpaSpecificationExecutor<AdminAuditLog> {
}
