package api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(
    name = "price_alerts",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "asset_id"})
)
@Data
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private CryptoAsset asset;

    @Column(name = "threshold_high")
    private Double thresholdHigh;

    @Column(name = "threshold_low")
    private Double thresholdLow;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "last_triggered_high_at")
    private Instant lastTriggeredHighAt;

    @Column(name = "last_triggered_low_at")
    private Instant lastTriggeredLowAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }
}
