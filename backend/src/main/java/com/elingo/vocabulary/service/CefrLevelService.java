package com.elingo.vocabulary.service;

import com.elingo.vocabulary.dto.response.CefrLevelResponse;
import java.util.List;

public interface CefrLevelService {
    List<CefrLevelResponse> getAllCefrLevels(Long userId);
}
