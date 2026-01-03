package collector.dto;

import lombok.Data;

@Data
public class CoinGeckoCoinDto {

    private String id;               // "bitcoin"
    private String symbol;           // "btc"
    private String name;             // "Bitcoin"
    private Double current_price;    // 50000.0
    private Double market_cap;       // market cap
    private Double total_volume;     // volume 24h
    private Double price_change_24h;  // Changement du prix en 24h
    private String image;            // URL de l'icône
    private Integer market_cap_rank;  // Classement par market cap
}
