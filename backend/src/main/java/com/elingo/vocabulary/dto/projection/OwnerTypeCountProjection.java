package com.elingo.vocabulary.dto.projection;

import com.elingo.vocabulary.entity.OwnerType;

public interface OwnerTypeCountProjection {
    OwnerType getOwnerType();
    Long getCountVal();
}
