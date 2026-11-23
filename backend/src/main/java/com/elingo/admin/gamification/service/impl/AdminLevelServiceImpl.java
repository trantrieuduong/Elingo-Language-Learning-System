package com.elingo.admin.gamification.service.impl;

import com.elingo.admin.audit.annotation.Audited;
import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import com.elingo.admin.gamification.dto.request.LevelRequest;
import com.elingo.admin.gamification.dto.response.AdminLevelResponse;
import com.elingo.admin.gamification.service.AdminLevelService;
import com.elingo.gamification.service.LevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ADMIN-LEVEL-SERVICE")
public class AdminLevelServiceImpl implements AdminLevelService {

    private final LevelService levelService;

    @Override
    public List<AdminLevelResponse> getAllLevels() {
        return levelService.getAllLevelsForAdmin();
    }

    @Override
    public AdminLevelResponse getLevelByNo(Short levelNo) {
        return levelService.getLevelByNo(levelNo);
    }

    @Override
    @Audited(
            action = AdminAuditAction.LEVEL_CREATE,
            target = AdminAuditTargetType.LEVEL,
            targetId = "#result.levelNo",
            detail = "'Create level: ' + #result.levelNo + ' requiredXp=' + #request.requiredXp"
    )
    public AdminLevelResponse createLevel(LevelRequest request) {
        return levelService.createLevel(request);
    }

    @Override
    @Audited(
            action = AdminAuditAction.LEVEL_UPDATE,
            target = AdminAuditTargetType.LEVEL,
            targetId = "#levelNo",
            detail = "'Update level: ' + #levelNo"
    )
    public AdminLevelResponse updateLevel(Short levelNo, LevelRequest request) {
        return levelService.updateLevel(levelNo, request);
    }

    @Override
    @Audited(
            action = AdminAuditAction.LEVEL_DELETE,
            target = AdminAuditTargetType.LEVEL,
            targetId = "#levelNo"
    )
    public void deleteLevel(Short levelNo) {
        levelService.deleteLevel(levelNo);
    }
}
