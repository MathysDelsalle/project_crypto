package api.service;

import api.model.CryptoAsset;
import api.model.User;
import api.repository.CryptoAssetRepository;
import api.repository.UserFavoritesRepository;
import api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserSpaceServiceTest {

    private UserRepository userRepository;
    private CryptoAssetRepository assetRepository;
    private UserFavoritesRepository favoritesRepository;

    private UserSpaceService service;

    @BeforeEach
    void setup() {
        userRepository = mock(UserRepository.class);
        assetRepository = mock(CryptoAssetRepository.class);
        favoritesRepository = mock(UserFavoritesRepository.class);
        service = new UserSpaceService(userRepository, assetRepository, favoritesRepository);
    }

    @Test
    void getUserOrThrow_throwsWhenMissing() {
        when(userRepository.findByUsername("mathys")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.getUserOrThrow("mathys"));

        assertTrue(ex.getMessage().contains("Utilisateur introuvable"));
    }

    @Test
    void getBalance_returnsUserBalance() {
        User u = new User();
        u.setId(1L);
        u.setBalance(123.45);

        when(userRepository.findByUsername("mathys")).thenReturn(Optional.of(u));

        assertEquals(123.45, service.getBalance("mathys"));
    }

    @Test
    void addBalance_noopWhenAmountNonPositive() {
        User u = new User();
        u.setId(1L);
        u.setBalance(100.0);

        when(userRepository.findByUsername("mathys")).thenReturn(Optional.of(u));

        double out = service.addBalance("mathys", 0);

        assertEquals(100.0, out);
        verify(userRepository, never()).save(any());
    }

    @Test
    void addBalance_incrementsAndSaves() {
        User u = new User();
        u.setId(1L);
        u.setBalance(100.0);

        when(userRepository.findByUsername("mathys")).thenReturn(Optional.of(u));

        double out = service.addBalance("mathys", 50.0);

        assertEquals(150.0, out);
        verify(userRepository).save(u);
    }

    @Test
    void getFavorites_returnsExternalIds() {
        User u = new User();
        u.setId(1L);

        when(userRepository.findByUsername("mathys")).thenReturn(Optional.of(u));
        when(favoritesRepository.findFavoriteExternalIds(1L)).thenReturn(List.of("btc", "eth"));

        List<String> out = service.getFavorites("mathys");

        assertEquals(List.of("btc", "eth"), out);
    }

    @Test
    void addFavorite_throwsWhenAssetNotFound() {
        User u = new User();
        u.setId(1L);

        when(userRepository.findByUsername("mathys")).thenReturn(Optional.of(u));
        when(assetRepository.findByExternalId("btc")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addFavorite("mathys", "btc"));

        assertTrue(ex.getMessage().contains("Asset introuvable"));
        verify(favoritesRepository, never()).addFavorite(anyLong(), anyLong());
    }

    @Test
    void addFavorite_callsRepoWithUserIdAndAssetId() {
        User u = new User();
        u.setId(1L);

        CryptoAsset asset = new CryptoAsset();
        asset.setId(10L);

        when(userRepository.findByUsername("mathys")).thenReturn(Optional.of(u));
        when(assetRepository.findByExternalId("btc")).thenReturn(Optional.of(asset));

        service.addFavorite("mathys", "btc");

        verify(favoritesRepository).addFavorite(1L, 10L);
    }

    @Test
    void removeFavorite_callsRepoWithUserIdAndAssetId() {
        User u = new User();
        u.setId(1L);

        CryptoAsset asset = new CryptoAsset();
        asset.setId(10L);

        when(userRepository.findByUsername("mathys")).thenReturn(Optional.of(u));
        when(assetRepository.findByExternalId("btc")).thenReturn(Optional.of(asset));

        service.removeFavorite("mathys", "btc");

        verify(favoritesRepository).removeFavorite(1L, 10L);
    }
}
