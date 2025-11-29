package api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import api.model.CryptoAsset;

import java.util.Optional;

public interface CryptoAssetRepository extends JpaRepository<CryptoAsset, Long> {

    // Permet de retrouver une crypto par son symbole (ex: BTC)
    Optional<CryptoAsset> findByExternalId(String externalId);
}