package com.elingo.vocabulary.service.impl;

import com.elingo.vocabulary.dto.response.TagResponse;
import com.elingo.vocabulary.mapper.TagMapper;
import com.elingo.vocabulary.repository.TagRepository;
import com.elingo.vocabulary.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "TAG-SERVICE")
public class TagServiceImpl implements TagService {
    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags(Long userId) {
        log.info("Fetching all tags for userId={}", userId);
        List<TagResponse> tags = tagRepository.findAll().stream()
                .map(tagMapper::toTagResponse)
                .toList();
        log.info("Fetched {} tag(s) for userId={}", tags.size(), userId);
        return tags;
    }
}
