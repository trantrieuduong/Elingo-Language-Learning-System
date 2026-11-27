package com.elingo.community.service.impl;

import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.community.dto.projection.PostGroupCountProjection;
import com.elingo.community.dto.request.AdminPostGroupRequest;
import com.elingo.community.dto.response.AdminPostGroupResponse;
import com.elingo.community.dto.response.PostGroupResponse;
import com.elingo.community.entity.PostGroup;
import com.elingo.community.mapper.PostGroupMapper;
import com.elingo.community.repository.PostGroupRepository;
import com.elingo.community.repository.PostRepository;
import com.elingo.community.service.PostGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "POST-GROUP-SERVICE")
public class PostGroupServiceImpl implements PostGroupService {

    private final PostGroupRepository repository;
    private final PostRepository postRepository;
    private final PostGroupMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<PostGroupResponse> getPostGroups() {
        return repository.findAll().stream()
                .map(mapper::toPostGroupResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminPostGroupResponse> searchForAdmin(String keyword, Pageable pageable) {
        String normalizedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        Page<PostGroup> page = repository.searchForAdmin(normalizedKeyword, pageable);
        if (page.isEmpty()) {
            return PageResponse.of(page.map(g -> mapper.toAdminResponse(g, 0L)));
        }

        List<Long> groupIds = page.getContent().stream().map(PostGroup::getId).toList();
        Map<Long, Long> postCounts = postRepository.countPostsByGroupIds(groupIds).stream()
                .collect(Collectors.toMap(
                        PostGroupCountProjection::getGroupId,
                        PostGroupCountProjection::getPostCount
                ));

        return PageResponse.of(page.map(g -> mapper.toAdminResponse(g, postCounts.getOrDefault(g.getId(), 0L))));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPostGroupResponse getByIdForAdmin(Long groupId) {
        PostGroup group = repository.findById(groupId)
                .orElseThrow(() -> new AppException(AppError.POST_GROUP_NOT_FOUND));
        long postCount = postRepository.countByGroupId(groupId);
        return mapper.toAdminResponse(group, postCount);
    }

    @Override
    @Transactional
    public AdminPostGroupResponse createForAdmin(AdminPostGroupRequest request) {
        String trimmedName = request.groupName().trim();
        if (repository.existsByGroupNameIgnoreCase(trimmedName)) {
            throw new AppException(AppError.POST_GROUP_ALREADY_EXISTS);
        }

        AdminPostGroupRequest normalized = new AdminPostGroupRequest(
                trimmedName,
                request.description() != null ? request.description().trim() : null
        );
        PostGroup saved = repository.save(mapper.toEntity(normalized));
        log.info("Post group created groupId={}", saved.getId());
        return mapper.toAdminResponse(saved, 0L);
    }

    @Override
    @Transactional
    public AdminPostGroupResponse updateForAdmin(Long groupId, AdminPostGroupRequest request) {
        PostGroup group = repository.findById(groupId)
                .orElseThrow(() -> new AppException(AppError.POST_GROUP_NOT_FOUND));

        String trimmedName = request.groupName().trim();
        if (repository.existsByGroupNameIgnoreCaseAndIdNot(trimmedName, groupId)) {
            throw new AppException(AppError.POST_GROUP_ALREADY_EXISTS);
        }

        AdminPostGroupRequest normalized = new AdminPostGroupRequest(
                trimmedName,
                request.description() != null ? request.description().trim() : null
        );
        mapper.updateEntity(group, normalized);
        PostGroup saved = repository.save(group);
        long postCount = postRepository.countByGroupId(groupId);
        log.info("Post group updated groupId={}", groupId);
        return mapper.toAdminResponse(saved, postCount);
    }

    @Override
    @Transactional
    public void deleteForAdmin(Long groupId) {
        PostGroup group = repository.findById(groupId)
                .orElseThrow(() -> new AppException(AppError.POST_GROUP_NOT_FOUND));
        if (postRepository.existsByGroupId(groupId)) {
            throw new AppException(AppError.POST_GROUP_IN_USE);
        }
        repository.delete(group);
        log.info("Post group deleted groupId={}", groupId);
    }
}
