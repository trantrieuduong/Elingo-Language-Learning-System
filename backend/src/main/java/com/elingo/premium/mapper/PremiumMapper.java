package com.elingo.premium.mapper;

import com.elingo.premium.dto.request.CreatePremiumPlanRequest;
import com.elingo.premium.dto.request.UpdatePremiumPlanRequest;
import com.elingo.premium.dto.response.AdminPremiumPlanResponse;
import com.elingo.premium.dto.response.AdminTransactionDetailResponse;
import com.elingo.premium.dto.response.AdminTransactionResponse;
import com.elingo.premium.dto.response.AdminWebhookLogDetailResponse;
import com.elingo.premium.dto.response.AdminWebhookLogResponse;
import com.elingo.premium.dto.response.PremiumPlanResponse;
import com.elingo.premium.entity.PaymentTransaction;
import com.elingo.premium.entity.PaymentWebhookLog;
import com.elingo.premium.entity.PremiumPlan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PremiumMapper {
    PremiumPlanResponse toPremiumPlanResponse(PremiumPlan plan);
    List<PremiumPlanResponse> toPremiumPlanResponseList(List<PremiumPlan> plans);

    AdminPremiumPlanResponse toAdminPremiumPlanResponse(PremiumPlan plan);
    List<AdminPremiumPlanResponse> toAdminPremiumPlanResponseList(List<PremiumPlan> plans);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isActive", constant = "false")
    PremiumPlan toEntity(CreatePremiumPlanRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "durationDays", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    void updateEntity(@MappingTarget PremiumPlan plan, UpdatePremiumPlanRequest request);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.email", target = "userEmail")
    @Mapping(source = "plan.id", target = "planId")
    @Mapping(source = "plan.name", target = "planName")
    @Mapping(source = "provider.code", target = "providerCode")
    AdminTransactionResponse toAdminTransactionResponse(PaymentTransaction tx);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.email", target = "userEmail")
    @Mapping(source = "user.fullName", target = "userFullName")
    @Mapping(source = "plan.id", target = "planId")
    @Mapping(source = "plan.name", target = "planName")
    @Mapping(source = "plan.durationDays", target = "planDurationDays")
    @Mapping(source = "provider.code", target = "providerCode")
    @Mapping(source = "provider.name", target = "providerName")
    @Mapping(source = "subscription.id", target = "subscriptionId")
    @Mapping(source = "subscription.status", target = "subscriptionStatus")
    @Mapping(source = "subscription.startAt", target = "subscriptionStartAt")
    @Mapping(source = "subscription.endAt", target = "subscriptionEndAt")
    AdminTransactionDetailResponse toAdminTransactionDetailResponse(PaymentTransaction tx);

    @Mapping(source = "provider.id", target = "providerId")
    @Mapping(source = "provider.code", target = "providerCode")
    @Mapping(source = "transaction.id", target = "transactionId")
    AdminWebhookLogResponse toAdminWebhookLogResponse(PaymentWebhookLog log);

    @Mapping(source = "provider.id", target = "providerId")
    @Mapping(source = "provider.code", target = "providerCode")
    @Mapping(source = "transaction.id", target = "transactionId")
    AdminWebhookLogDetailResponse toAdminWebhookLogDetailResponse(PaymentWebhookLog log);
}
