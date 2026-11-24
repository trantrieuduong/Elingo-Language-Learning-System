package com.elingo.admin.gamification.service;

import com.elingo.gamification.dto.request.LevelRequest;
import com.elingo.gamification.dto.response.AdminLevelResponse;

import java.util.List;

public interface AdminLevelService {

    List<AdminLevelResponse> getAllLevels();

    AdminLevelResponse getLevelByNo(Short levelNo);

    AdminLevelResponse createLevel(LevelRequest request);

    AdminLevelResponse updateLevel(Short levelNo, LevelRequest request);

    void deleteLevel(Short levelNo);
}
