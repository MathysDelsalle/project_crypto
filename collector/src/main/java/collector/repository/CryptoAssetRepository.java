package collector.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import collector.model.CryptoAsset;

import java.util.Optional;

public interface CryptoAssetRepository extends JpaRepository<CryptoAsset, Long> {

    // Permet de retrouver une crypto par son nom (ex: Bitcoin)
    Optional<CryptoAsset> findByExternalId(String ExternalId);
}
