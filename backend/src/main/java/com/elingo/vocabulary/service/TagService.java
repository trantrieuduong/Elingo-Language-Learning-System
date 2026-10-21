package com.elingo.vocabulary.service;

import com.elingo.vocabulary.dto.response.TagResponse;
import java.util.List;

public interface TagService {
    List<TagResponse> getAllTags(Long userId);
}
