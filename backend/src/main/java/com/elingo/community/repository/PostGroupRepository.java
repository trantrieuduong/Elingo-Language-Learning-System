package com.elingo.community.repository;

import com.elingo.community.entity.PostGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostGroupRepository extends JpaRepository<PostGroup, Long> {

    boolean existsByGroupNameIgnoreCase(String groupName);

    boolean existsByGroupNameIgnoreCaseAndIdNot(String groupName, Long id);

    @Query("""
            SELECT g FROM PostGroup g
            WHERE (:keyword IS NULL OR LOWER(g.groupName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(COALESCE(g.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<PostGroup> searchForAdmin(@Param("keyword") String keyword, Pageable pageable);
}
