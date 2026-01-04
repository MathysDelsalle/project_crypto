package api.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(
    name = "user_holdings",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "asset_id"})
)
@Data
public class UserHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="asset_id", nullable = false)
    private CryptoAsset asset;

    @Column(nullable = false)
    private Double quantity = 0.0;
}
