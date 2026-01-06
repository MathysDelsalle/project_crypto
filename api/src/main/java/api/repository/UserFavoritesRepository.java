package api.repository;

import api.model.UserFavorite;
import api.model.UserFavoriteId;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface UserFavoritesRepository extends Repository<UserFavorite, UserFavoriteId> {

    @Query(value = """
        SELECT a.external_id
        FROM user_favorites uf
        JOIN crypto_assets a ON a.id = uf.asset_id
        WHERE uf.user_id = ?1
        ORDER BY uf.created_at DESC
        """, nativeQuery = true)
    List<String> findFavoriteExternalIds(Long userId);

    @Query(value = """
        SELECT EXISTS(
          SELECT 1
          FROM user_favorites uf
          WHERE uf.user_id = ?1 AND uf.asset_id = ?2
        )
        """, nativeQuery = true)
    boolean exists(Long userId, Long assetId);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO user_favorites (user_id, asset_id)
        VALUES (?1, ?2)
        ON CONFLICT DO NOTHING
        """, nativeQuery = true)
    void addFavorite(Long userId, Long assetId);

    @Modifying
    @Transactional
    @Query(value = """
        DELETE FROM user_favorites
        WHERE user_id = ?1 AND asset_id = ?2
        """, nativeQuery = true)
    void removeFavorite(Long userId, Long assetId);
}
