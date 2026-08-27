package br.dev.bielsolosos.biscraper.domain.monitoring.model;

import br.dev.bielsolosos.biscraper.core.enums.ExecutionStatus;
import br.dev.bielsolosos.biscraper.core.enums.Vendor;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "scraping_executions")
public class ScrapingExecution {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webhook_event_id")
    private WebhookEvent webhookEvent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_monitor_id", nullable = false)
    private ProductMonitor productMonitor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "search_query_id")
    private MonitorSearchQuery searchQuery;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Vendor vendor = Vendor.OLX;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ExecutionStatus status = ExecutionStatus.PENDING;

    @Builder.Default
    @Column(name = "total_found", nullable = false)
    private int totalFound = 0;

    @Builder.Default
    @Column(name = "new_items_count", nullable = false)
    private int newItemsCount = 0;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Builder.Default
    @Column(name = "used_fallback", nullable = false)
    private boolean usedFallback = false;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "executed_at", nullable = false)
    private OffsetDateTime executedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
