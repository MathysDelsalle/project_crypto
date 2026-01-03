package collector.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "crypto_assets")
@Data
public class CryptoAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Id CoinGecko ex: "bitcoin"
    @Column(nullable = false, unique = true)
    private String externalId;

    private String symbol;

    private String name;

    private Double currentPrice;

    private Double marketCap;

    private Double totalVolume;

    private Double price_change_24h; 

    private String imageUrl;

    @Column(name = "market_cap_rank")
    private Integer marketCapRank;
}
