package com.elingo.community.repository;

import com.elingo.community.entity.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostMediaRepository extends JpaRepository<PostMedia, Long> {
    List<PostMedia> findByPostIdOrderByOrderAsc(Long postId);

    void deleteByPostId(Long postId);
}

