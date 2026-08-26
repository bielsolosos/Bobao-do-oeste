package br.dev.bielsolosos.biscraper.domain.monitoring.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "monitor_search_queries")
public class MonitorSearchQuery {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_monitor_id", nullable = false)
    private ProductMonitor productMonitor;

    @NotBlank(message = "Termo de busca não pode ser vazio.")
    @Column(name = "query_term", nullable = false, length = 200)
    private String queryTerm;

    @Column(name = "min_price", precision = 12, scale = 2)
    private BigDecimal minPrice;

    @Column(name = "max_price", precision = 12, scale = 2)
    private BigDecimal maxPrice;

    @Column(name = "state_filter", length = 10)
    private String stateFilter;

    @Column(name = "region_filter", length = 50)
    private String regionFilter;

    @Column(name = "category_slug", length = 100)
    private String categorySlug;

    @Builder.Default
    @Column(name = "require_delivery", nullable = false)
    private boolean requireDelivery = false;

    @Builder.Default
    @Column(name = "max_pages", nullable = false)
    private int maxPages = 1;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
