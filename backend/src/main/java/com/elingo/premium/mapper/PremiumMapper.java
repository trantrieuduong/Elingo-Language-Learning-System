package com.elingo.premium.mapper;

import com.elingo.premium.dto.response.PremiumPlanResponse;
import com.elingo.premium.entity.PremiumPlan;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PremiumMapper {
    PremiumPlanResponse toPremiumPlanResponse(PremiumPlan plan);
    List<PremiumPlanResponse> toPremiumPlanResponseList(List<PremiumPlan> plans);
}
