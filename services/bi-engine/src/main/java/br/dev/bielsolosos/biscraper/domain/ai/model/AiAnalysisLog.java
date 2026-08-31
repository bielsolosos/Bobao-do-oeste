package br.dev.bielsolosos.biscraper.domain.ai.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapingExecution;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ai_analysis_logs")
public class AiAnalysisLog {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_monitor_id")
    private ProductMonitor productMonitor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scraping_execution_id")
    private ScrapingExecution scrapingExecution;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Builder.Default
    @Column(nullable = false, length = 50)
    private String vendor = "GEMINI";

    @Builder.Default
    @Column(name = "items_count", nullable = false)
    private int itemsCount = 0;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "user_prompt", columnDefinition = "TEXT")
    private String userPrompt;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Builder.Default
    @Column(nullable = false, length = 50)
    private String status = "SUCCESS";

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
