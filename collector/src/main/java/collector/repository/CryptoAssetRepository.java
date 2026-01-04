package collector.repository;

import collector.model.CryptoAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CryptoAssetRepository extends JpaRepository<CryptoAsset, Long> {

    Optional<CryptoAsset> findByExternalId(String externalId);

    List<CryptoAsset> findTop100ByOrderByMarketCapRankAsc();
}
