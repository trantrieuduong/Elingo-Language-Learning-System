package com.elingo.premium.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import com.elingo.common.entity.BaseEntity;

@Entity
@Table(
        name = "payment_webhook_logs",
        indexes = {
                @Index(name = "idx_webhook_transaction", columnList = "transaction_id")
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentWebhookLog extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    PaymentProvider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    PaymentTransaction transaction;

    @Column(name = "event_id", length = 150)
    String eventId;

    @Column(columnDefinition = "json", nullable = false)
    String payload;

    @Column(name = "signature_valid")
    Boolean signatureValid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    WebhookLogStatus status = WebhookLogStatus.RECEIVED;

    @CreationTimestamp
    @Column(name = "received_at", updatable = false)
    LocalDateTime receivedAt;

    @Column(name = "processed_at")
    LocalDateTime processedAt;
}
