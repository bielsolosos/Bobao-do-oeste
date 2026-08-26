package br.dev.bielsolosos.biscraper.domain.monitoring.model;

import br.dev.bielsolosos.biscraper.domain.monitoring.model.enums.AnalysisType;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.enums.Vendor;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "product_monitors")
public class ProductMonitor {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank(message = "Nome do monitor não pode ser vazio.")
    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_type", nullable = false, length = 50)
    private AnalysisType analysisType = AnalysisType.SIMPLE;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "target_vendor", nullable = false, length = 50)
    private Vendor targetVendor = Vendor.OLX;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "cron_expression", length = 50)
    private String cronExpression;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "expected_specs", columnDefinition = "jsonb")
    private JsonNode expectedSpecs;

    @Column(name = "last_scraped_at")
    private OffsetDateTime lastScrapedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "productMonitor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MonitorSearchQuery> searchQueries = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "productMonitor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScrapedListing> listings = new ArrayList<>();


    public void addSearchQuery(MonitorSearchQuery query) {
        searchQueries.add(query);
        query.setProductMonitor(this);
    }

    public void removeSearchQuery(MonitorSearchQuery query) {
        searchQueries.remove(query);
        query.setProductMonitor(null);
    }
}
