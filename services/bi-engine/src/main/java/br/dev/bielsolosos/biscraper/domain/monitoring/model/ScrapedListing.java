package br.dev.bielsolosos.biscraper.domain.monitoring.model;

import br.dev.bielsolosos.biscraper.core.enums.MatchTier;
import br.dev.bielsolosos.biscraper.core.enums.Vendor;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "scraped_listings",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_monitor_vendor_listing",
            columnNames = {"product_monitor_id", "vendor", "vendor_listing_id"}
        )
    }
)
public class ScrapedListing {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_monitor_id", nullable = false)
    private ProductMonitor productMonitor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_execution_id")
    private ScrapingExecution lastExecution;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Vendor vendor = Vendor.OLX;

    @NotBlank(message = "ID nativo do anúncio não pode ser vazio.")
    @Column(name = "vendor_listing_id", nullable = false, length = 100)
    private String vendorListingId;

    @NotBlank(message = "Título do anúncio não pode ser vazio.")
    @Column(nullable = false, length = 300)
    private String title;

    @NotBlank(message = "URL do anúncio não pode ser vazia.")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "current_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "original_price", precision = 12, scale = 2)
    private BigDecimal originalPrice;

    @Column(length = 10)
    private String state;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String neighborhood;

    @Builder.Default
    @Column(name = "has_delivery", nullable = false)
    private boolean hasDelivery = false;

    @Column(name = "delivery_type", length = 50)
    private String deliveryType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> images;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "match_tier", nullable = false, length = 50)
    private MatchTier matchTier = MatchTier.NONE;

    @Builder.Default
    @Column(name = "match_score", precision = 5, scale = 2)
    private BigDecimal matchScore = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extracted_specs", columnDefinition = "jsonb")
    private Map<String, Object> extractedSpecs;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private OffsetDateTime firstSeenAt;

    @UpdateTimestamp
    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt;
}
