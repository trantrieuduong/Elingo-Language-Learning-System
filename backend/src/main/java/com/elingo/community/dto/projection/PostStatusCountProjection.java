package com.elingo.community.dto.projection;

import com.elingo.community.entity.PostStatus;

public interface PostStatusCountProjection {
    PostStatus getStatus();
    Long getCountVal();
}
