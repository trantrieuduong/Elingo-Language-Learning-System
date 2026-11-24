package com.elingo.gamification.service;

import com.elingo.gamification.dto.request.LevelRequest;
import com.elingo.gamification.dto.response.AdminLevelResponse;
import com.elingo.gamification.dto.response.LevelResponse;
import com.elingo.gamification.dto.response.UserLevelProgressResponse;
import com.elingo.gamification.entity.Level;

import java.util.List;

public interface LevelService {

    /**
     * Lấy danh sách toàn bộ các cấp độ trong hệ thống (Roadmap cấp độ).
     */
    List<LevelResponse> getAllLevels();

    /**
     * Lấy chi tiết tiến độ cấp độ của người dùng (cấp hiện tại, tổng XP, cấp tiếp theo, XP cần thêm và % tiến độ).
     */
    UserLevelProgressResponse getUserLevelProgress(Long userId);

    // Admin
    List<AdminLevelResponse> getAllLevelsForAdmin();

    AdminLevelResponse getLevelByNo(Short levelNo);

    AdminLevelResponse createLevel(LevelRequest request);

    AdminLevelResponse updateLevel(Short levelNo, LevelRequest request);

    void deleteLevel(Short levelNo);
}
