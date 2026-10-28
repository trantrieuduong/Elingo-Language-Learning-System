package com.elingo.community.service.impl;

import com.elingo.community.dto.response.PostGroupResponse;
import com.elingo.community.mapper.PostGroupMapper;
import com.elingo.community.repository.PostGroupRepository;
import com.elingo.community.service.PostGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "POST-GROUP-SERVICE")
public class PostGroupServiceImpl implements PostGroupService {

    private final PostGroupRepository repository;
    private final PostGroupMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<PostGroupResponse> getPostGroups() {
        return repository.findAll().stream()
                .map(mapper::toPostGroupResponse).toList();
    }
}
