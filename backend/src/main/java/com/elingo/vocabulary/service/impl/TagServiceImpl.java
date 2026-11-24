package com.elingo.vocabulary.service.impl;

import com.elingo.vocabulary.dto.request.TagRequest;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.vocabulary.dto.response.TagResponse;
import com.elingo.vocabulary.entity.Tag;
import com.elingo.vocabulary.mapper.TagMapper;
import com.elingo.vocabulary.repository.TagRepository;
import com.elingo.vocabulary.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
    public List<TagResponse> getAllTags() {
        return tagRepository.findAll().stream()
                .map(tagMapper::toTagResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TagResponse getTagById(Long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new AppException(AppError.TAG_NOT_FOUND));
        return tagMapper.toTagResponse(tag);
    }

    @Override
    @Transactional
    public TagResponse createTag(TagRequest request) {
        if (tagRepository.existsByLabelIgnoreCase(request.label())) {
            throw new AppException(AppError.TAG_LABEL_EXISTED);
        }

        Tag tag = Tag.builder()
                .label(request.label())
                .build();

        Tag savedTag = tagRepository.save(tag);
        return tagMapper.toTagResponse(savedTag);
    }

    @Override
    @Transactional
    public TagResponse updateTag(Long tagId, TagRequest request) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new AppException(AppError.TAG_NOT_FOUND));

        if (tagRepository.existsByLabelIgnoreCaseAndIdNot(request.label(), tagId)) {
            throw new AppException(AppError.TAG_LABEL_EXISTED);
        }

        tag.setLabel(request.label());
        Tag updatedTag = tagRepository.save(tag);
        return tagMapper.toTagResponse(updatedTag);
    }

    @Override
    @Transactional
    public void deleteTag(Long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new AppException(AppError.TAG_NOT_FOUND));

        try {
            tagRepository.delete(tag);
            tagRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            log.warn("Cannot delete tag id={} as it is referenced by decks or lessons: {}", tagId, ex.getMessage());
            throw new AppException(AppError.TAG_IN_USE);
        }
    }
}
