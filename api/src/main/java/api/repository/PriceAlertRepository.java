package api.repository;

import api.model.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

    List<PriceAlert> findByUserId(Long userId);

    Optional<PriceAlert> findByUserIdAndAsset_Id(Long userId, Long assetId);

    List<PriceAlert> findByActiveTrue();

    @Modifying
    @Transactional
    long deleteByUserIdAndAsset_Id(Long userId, Long assetId);
}
