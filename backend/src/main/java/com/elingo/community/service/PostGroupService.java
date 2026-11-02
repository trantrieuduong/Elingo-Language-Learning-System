package com.elingo.community.service;

import com.elingo.community.dto.response.PostGroupResponse;

import java.util.List;

public interface PostGroupService {
    List<PostGroupResponse> getPostGroups();
}
