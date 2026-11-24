package com.elingo.community.dto.projection;

public interface PostGroupCountProjection {
    Long getGroupId();
    String getGroupName();
    Long getPostCount();
}
