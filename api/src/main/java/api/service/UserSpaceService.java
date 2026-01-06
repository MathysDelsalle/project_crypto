package api.service;

import api.repository.CryptoAssetRepository;
import api.repository.UserFavoritesRepository;
import api.repository.UserRepository;
import api.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserSpaceService {

    private final UserRepository userRepository;
    private final CryptoAssetRepository assetRepository;
    private final UserFavoritesRepository favoritesRepository;

    public User getUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable: " + username));
    }

    public double getBalance(String username) {
        return getUserOrThrow(username).getBalance();
    }

    @Transactional
    public double addBalance(String username, double amount) {
        if (amount <= 0) return getBalance(username);

        User u = getUserOrThrow(username);
        u.setBalance(u.getBalance() + amount);
        userRepository.save(u);
        return u.getBalance();
    }

    public List<String> getFavorites(String username) {
        User u = getUserOrThrow(username);
        return favoritesRepository.findFavoriteExternalIds(u.getId());
    }

    @Transactional
    public void addFavorite(String username, String externalId) {
        User u = getUserOrThrow(username);
        var asset = assetRepository.findByExternalId(externalId)
                .orElseThrow(() -> new IllegalArgumentException("Asset introuvable: " + externalId));
        favoritesRepository.addFavorite(u.getId(), asset.getId());
    }

    @Transactional
    public void removeFavorite(String username, String externalId) {
        User u = getUserOrThrow(username);
        var asset = assetRepository.findByExternalId(externalId)
                .orElseThrow(() -> new IllegalArgumentException("Asset introuvable: " + externalId));
        favoritesRepository.removeFavorite(u.getId(), asset.getId());
    }
}
