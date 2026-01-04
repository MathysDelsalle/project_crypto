package collector.repository;

import collector.model.CryptoPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface CryptoPriceHistoryRepository extends JpaRepository<CryptoPriceHistory, Long> {

    /**
     * True si la table crypto_price_history est vide.
     */
    @Query(value = "SELECT COUNT(*) = 0 FROM crypto_price_history", nativeQuery = true)
    boolean isEmptyTable();

    /**
     * True si un asset a déjà au moins un point d'historique pour une devise (vs_currency).
     * Utilisé pour savoir si on doit bootstrapper l'historique.
     */
    boolean existsByAssetIdAndVsCurrency(Long assetId, String vsCurrency);

    /**
     * Nombre d'assets distincts ayant au moins un point d'historique pour une devise.
     * Pratique pour savoir si le Top100 est déjà bootstrappé.
     */
    @Query(value = "SELECT COUNT(DISTINCT asset_id) FROM crypto_price_history WHERE vs_currency = ?1", nativeQuery = true)
    long countDistinctAssetsByVsCurrency(String vsCurrency);

    /**
     * Série de prix (ts, price) depuis un instant donné (ex: now - 7 days), triée par date croissante.
     * Retour natif: Object[]{Timestamp ts, Double price}.
     */
    @Query(value = """
        SELECT ts, price
        FROM crypto_price_history
        WHERE asset_id = ?1
          AND vs_currency = ?2
          AND ts >= ?3
        ORDER BY ts ASC
        """, nativeQuery = true)
    List<Object[]> findPriceSeries(Long assetId, String vsCurrency, Instant fromTs);

    /**
     * Insert/Update d'un point d'historique (PostgreSQL ON CONFLICT).
     * Nécessite une contrainte UNIQUE (asset_id, vs_currency, ts) en base.
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO crypto_price_history (asset_id, vs_currency, ts, price, market_cap, total_volume)
        VALUES (?1, ?2, ?3, ?4, ?5, ?6)
        ON CONFLICT (asset_id, vs_currency, ts)
        DO UPDATE SET
          price = EXCLUDED.price,
          market_cap = EXCLUDED.market_cap,
          total_volume = EXCLUDED.total_volume
        """, nativeQuery = true)
    void upsertPoint(
        Long assetId,
        String vsCurrency,
        Instant ts,
        Double price,
        Double marketCap,
        Double totalVolume
    );
}
