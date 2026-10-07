package com.elingo.vocabulary.service.impl;

import com.elingo.vocabulary.dto.response.CefrLevelResponse;
import com.elingo.vocabulary.mapper.CefrLevelMapper;
import com.elingo.vocabulary.repository.CefrLevelRepository;
import com.elingo.vocabulary.service.CefrLevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "CEFR-LEVEL-SERVICE")
public class CefrLevelServiceImpl implements CefrLevelService {
    private final CefrLevelRepository cefrLevelRepository;
    private final CefrLevelMapper cefrLevelMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CefrLevelResponse> getAllCefrLevels(Long userId) {
        List<CefrLevelResponse> cefrLevels = cefrLevelRepository.findAll().stream()
                .map(cefrLevelMapper::toCefrLevelResponse)
                .toList();
        log.info("CEFR levels fetched count={}", cefrLevels.size());
        return cefrLevels;
    }
}
