package collector.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(
    name = "crypto_price_history",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_price_history_point",
        columnNames = {"asset_id", "vs_currency", "ts"}
    )
)
public class CryptoPriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "vs_currency", nullable = false, length = 10)
    private String vsCurrency;

    @Column(name = "ts", nullable = false)
    private Instant ts;

    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "market_cap")
    private Double marketCap;

    @Column(name = "total_volume")
    private Double totalVolume;
}
