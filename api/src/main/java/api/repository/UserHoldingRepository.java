package api.repository;

import api.model.UserHolding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserHoldingRepository extends JpaRepository<UserHolding, Long> {
    List<UserHolding> findByUserId(Long userId);
    Optional<UserHolding> findByUserIdAndAsset_Id(Long userId, Long assetId);
}
