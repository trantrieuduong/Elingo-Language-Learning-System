package com.elingo.vocabulary.service;

import com.elingo.vocabulary.dto.request.TagRequest;
import com.elingo.vocabulary.dto.response.TagResponse;
import java.util.List;

public interface TagService {
    List<TagResponse> getAllTags();
    TagResponse getTagById(Long tagId);
    TagResponse createTag(TagRequest request);
    TagResponse updateTag(Long tagId, TagRequest request);
    void deleteTag(Long tagId);
}
