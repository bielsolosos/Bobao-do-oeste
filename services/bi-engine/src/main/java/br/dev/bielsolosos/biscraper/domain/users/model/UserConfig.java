package br.dev.bielsolosos.biscraper.domain.users.model;

import br.dev.bielsolosos.biscraper.core.enums.ModelVendorEnum;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_configs")
public class UserConfig {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "ai_vendor", nullable = false, length = 50)
    @Builder.Default
    private ModelVendorEnum aiVendor = ModelVendorEnum.GEMINI;

    @NotBlank
    @Column(name = "cheap_model", nullable = false, length = 100)
    @Builder.Default
    private String cheapModel = "gemini-2.5-flash";

    @NotBlank
    @Column(name = "strong_model", nullable = false, length = 100)
    @Builder.Default
    private String strongModel = "gemini-2.5-pro";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
