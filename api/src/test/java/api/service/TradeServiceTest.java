package api.service;

import api.dto.HoldingResponse;
import api.model.CryptoAsset;
import api.model.User;
import api.model.UserHolding;
import api.repository.CryptoAssetRepository;
import api.repository.UserHoldingRepository;
import api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TradeServiceTest {

    private UserRepository userRepository;
    private CryptoAssetRepository assetRepository;
    private UserHoldingRepository holdingRepository;

    private TradeService service;

    @BeforeEach
    void setup() {
        userRepository = mock(UserRepository.class);
        assetRepository = mock(CryptoAssetRepository.class);
        holdingRepository = mock(UserHoldingRepository.class);
        service = new TradeService(userRepository, assetRepository, holdingRepository);
    }

    @Test
    void getHoldings_mapsEntitiesToDto() {
        User user = new User();
        user.setId(1L);

        CryptoAsset asset = new CryptoAsset();
        asset.setId(10L);
        asset.setExternalId("btc");
        asset.setName("Bitcoin");
        asset.setSymbol("BTC");

        UserHolding holding = new UserHolding();
        holding.setUserId(1L);
        holding.setAsset(asset);
        holding.setQuantity(2.5);

        when(userRepository.findByUsername("mathys")).thenReturn(Optional.of(user));
        when(holdingRepository.findByUserId(1L)).thenReturn(List.of(holding));

        List<HoldingResponse> out = service.getHoldings("mathys");

        assertEquals(1, out.size());
        HoldingResponse r = out.get(0);
        assertEquals("btc", r.getExternalId());
        assertEquals("Bitcoin", r.getName());
        assertEquals("BTC", r.getSymbol());
        assertEquals(2.5, r.getQuantity());
    }

    @Test
    void buy_rejectsNonPositiveQty() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.buy("mathys", "btc", 0));
        assertEquals("qty must be > 0", ex.getMessage());
        verifyNoInteractions(userRepository, assetRepository, holdingRepository);
    }

    @Test
    void buy_failsWhenUserNotFound() {
        when(userRepository.findByUsername("mathys")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.buy("mathys", "btc", 1));
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void buy_failsWhenAssetUnknown() {
        User user = new User();
        user.setId(1L);
        user.setBalance(1000.0);

        when(userRepository.findByUsername("mathys")).thenReturn(Optional.of(user));
        when(assetRepository.findByExternalId("btc")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.buy("mathys", "btc", 1));
        assertTrue(ex.getMessage().startsWith("Unknown crypto: "));
    }

    @Test
    void buy_failsWhenPriceUnavailable_nullOrZeroOrNegative() {
        User user = new User();
        user.setId(1L);
        user.setBalance(1000.0);

        CryptoAsset asset = new CryptoAsset();
        asset.setId(10L);
        asset.setExternalId("btc");
        asset.setCurrentPrice(null);

        when(userRepository.findByUsername("mathys")).thenReturn(Optional.of(user));
        when(assetRepository.findByExternalId("btc")).thenReturn(Optional.of(asset));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.buy("mathys", "btc", 1));
        assertEquals("Price unavailable", ex.getMessage());
    }

    @Test
    void buy_failsWhenInsufficientBalance() {
        User user = new User();
        user.setId(1L);
        user.setBalance(10.0);

        CryptoAsset asset = new CryptoAsset();
        asset.setId(10L);
        asset.setExternalId("btc");
        asset.setCurrentPrice(100.0);

        when(userRepository.findByUsername("mathys")).thenReturn(Optional.of(user));
        when(assetRepository.findByExternalId("btc")).thenReturn(Optional.of(asset));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.buy("mathys", "btc", 1));
        assertEquals("Solde insuffisant", ex.getMessage());
    }

    @Test
    void buy_createsHoldingIfMissing_andDebitsBalance() {
        User user = new User();
        user.setId(1L);
        user.setBalance(1000.0);

        CryptoAsset asset = new CryptoAsset();
        asset.setId(10L);
        asset.setExternalId("btc");
        asset.setCurrentPrice(100.0);

        when(userRepository.findByUsername("mathys")).thenReturn(Optional.of(user));
        when(assetRepository.findByExternalId("btc")).thenReturn(Optional.of(asset));
        when(holdingRepository.findByUserIdAndAsset_Id(1L, 10L)).thenReturn(Optional.empty());

        double newBalance = service.buy("mathys", "btc", 2); // cost 200

        assertEquals(800.0, newBalance);

        // user saved with new balance
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(800.0, userCaptor.getValue().getBalance());

        // holding created and saved
        ArgumentCaptor<UserHolding> holdingCaptor = ArgumentCaptor.forClass(UserHolding.class);
        verify(holdingRepository).save(holdingCaptor.capture());
        assertEquals(1L, holdingCaptor.getValue().getUserId());
        assertEquals(2.0, holdingCaptor.getValue().getQuantity());
        assertEquals(asset, holdingCaptor.getValue().getAsset());
    }

    @Test
    void sell_rejectsNonPositiveQty() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.sell("mathys", "btc", -1));
        assertEquals("qty must be > 0", ex.getMessage());
    }

    @Test
    void sell_failsWhenNoHolding() {
        User user = new User();
        user.setId(1L);
        user.setBalance(1000.0);

        CryptoAsset asset = new CryptoAsset();
        asset.setId(10L);
        asset.setExternalId("btc");
        asset.setCurrentPrice(100.0);

        when(userRepository.findByUsername("mathys")).thenReturn(Optional.of(user));
        when(assetRepository.findByExternalId("btc")).thenReturn(Optional.of(asset));
        when(holdingRepository.findByUserIdAndAsset_Id(1L, 10L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.sell("mathys", "btc", 1));
        assertEquals("Aucune possession pour cette crypto", ex.getMessage());
    }

    @Test
    void sell_failsWhenNotEnoughQuantity() {
        User user = new User();
        user.setId(1L);
        user.setBalance(1000.0);

        CryptoAsset asset = new CryptoAsset();
        asset.setId(10L);
        asset.setExternalId("btc");
        asset.setCurrentPrice(100.0);

        UserHolding holding = new UserHolding();
        holding.setUserId(1L);
        holding.setAsset(asset);
        holding.setQuantity(1.0);

        when(userRepository.findByUsername("mathys")).thenReturn(Optional.of(user));
        when(assetRepository.findByExternalId("btc")).thenReturn(Optional.of(asset));
        when(holdingRepository.findByUserIdAndAsset_Id(1L, 10L)).thenReturn(Optional.of(holding));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.sell("mathys", "btc", 2));
        assertEquals("Quantité insuffisante", ex.getMessage());
    }

    @Test
    void sell_deletesHoldingWhenQuantityBecomesZero_andCreditsBalance() {
        User user = new User();
        user.setId(1L);
        user.setBalance(1000.0);

        CryptoAsset asset = new CryptoAsset();
        asset.setId(10L);
        asset.setExternalId("btc");
        asset.setCurrentPrice(100.0);

        UserHolding holding = new UserHolding();
        holding.setUserId(1L);
        holding.setAsset(asset);
        holding.setQuantity(2.0);

        when(userRepository.findByUsername("mathys")).thenReturn(Optional.of(user));
        when(assetRepository.findByExternalId("btc")).thenReturn(Optional.of(asset));
        when(holdingRepository.findByUserIdAndAsset_Id(1L, 10L)).thenReturn(Optional.of(holding));

        double newBalance = service.sell("mathys", "btc", 2); // gain 200

        assertEquals(1200.0, newBalance);
        verify(holdingRepository).delete(holding);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(1200.0, userCaptor.getValue().getBalance());
    }

    @Test
    void sell_savesHoldingWhenRemainingQuantityPositive() {
        User user = new User();
        user.setId(1L);
        user.setBalance(1000.0);

        CryptoAsset asset = new CryptoAsset();
        asset.setId(10L);
        asset.setExternalId("btc");
        asset.setCurrentPrice(100.0);

        UserHolding holding = new UserHolding();
        holding.setUserId(1L);
        holding.setAsset(asset);
        holding.setQuantity(5.0);

        when(userRepository.findByUsername("mathys")).thenReturn(Optional.of(user));
        when(assetRepository.findByExternalId("btc")).thenReturn(Optional.of(asset));
        when(holdingRepository.findByUserIdAndAsset_Id(1L, 10L)).thenReturn(Optional.of(holding));

        double newBalance = service.sell("mathys", "btc", 2); // remaining 3, gain 200

        assertEquals(1200.0, newBalance);

        ArgumentCaptor<UserHolding> holdingCaptor = ArgumentCaptor.forClass(UserHolding.class);
        verify(holdingRepository).save(holdingCaptor.capture());
        assertEquals(3.0, holdingCaptor.getValue().getQuantity());
    }
}
