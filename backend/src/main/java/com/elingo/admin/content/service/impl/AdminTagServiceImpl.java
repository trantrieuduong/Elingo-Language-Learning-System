package com.elingo.admin.content.service.impl;

import com.elingo.admin.audit.annotation.Audited;
import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import com.elingo.vocabulary.dto.request.TagRequest;
import com.elingo.admin.content.service.AdminTagService;
import com.elingo.vocabulary.dto.response.TagResponse;
import com.elingo.vocabulary.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ADMIN-TAG-SERVICE")
public class AdminTagServiceImpl implements AdminTagService {

    private final TagService tagService;

    @Override
    public List<TagResponse> getAllTags() {
        return tagService.getAllTags();
    }

    @Override
    public TagResponse getTagById(Long tagId) {
        return tagService.getTagById(tagId);
    }

    @Override
    @Audited(action = AdminAuditAction.TAG_CREATE,
            target = AdminAuditTargetType.TAG,
            targetId = "#result.id",
            detail = "'Label: ' + #request.label")
    public TagResponse createTag(TagRequest request) {
        TagResponse created = tagService.createTag(request);
        log.info("Tag created tagId={}", created.id());
        return created;
    }

    @Override
    @Audited(action = AdminAuditAction.TAG_UPDATE,
            target = AdminAuditTargetType.TAG,
            targetId = "#tagId",
            detail = "'Update label: ' + #request.label")
    public TagResponse updateTag(Long tagId, TagRequest request) {
        TagResponse updated = tagService.updateTag(tagId, request);
        log.info("Tag updated tagId={}", tagId);
        return updated;
    }

    @Override
    @Audited(action = AdminAuditAction.TAG_DELETE,
            target = AdminAuditTargetType.TAG,
            targetId = "#tagId")
    public void deleteTag(Long tagId) {
        tagService.deleteTag(tagId);
        log.info("Tag deleted tagId={}", tagId);
    }
}
