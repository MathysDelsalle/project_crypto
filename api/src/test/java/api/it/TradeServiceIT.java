package api.it;

import api.model.CryptoAsset;
import api.model.Role;
import api.model.User;
import api.repository.*;
import api.service.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TradeServiceIT extends AbstractPostgresIT {

    @Autowired private TradeService tradeService;

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CryptoAssetRepository cryptoAssetRepository;
    @Autowired private UserHoldingRepository userHoldingRepository;

    // ✅ important pour éviter la violation FK sur crypto_assets
    @Autowired private PriceAlertRepository priceAlertRepository;

    private User user;
    private CryptoAsset asset;

    @BeforeEach
    void setup() {
        // ✅ Nettoyage dans l’ordre FK (children -> parents)
        priceAlertRepository.deleteAll();
        userHoldingRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        cryptoAssetRepository.deleteAll();

        Role roleUser = new Role();
        roleUser.setName("ROLE_USER");
        roleUser = roleRepository.save(roleUser);

        String uniq = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        user = User.builder()
                .username("user_it_" + uniq)
                .email("user_it_" + uniq + "@test.com")
                .password("encoded")
                .enabled(true)
                .balance(1000.0)
                .build();
        user.getRoles().add(roleUser);
        user = userRepository.save(user);

        asset = CryptoAsset.builder()
                .externalId("btc_" + uniq) // ✅ unique à cause du @Column(unique=true)
                .symbol("BTC")
                .name("Bitcoin")
                .currentPrice(100.0)
                .build();
        asset = cryptoAssetRepository.save(asset);
    }

    @Test
    void buy_shouldCreateHolding_andDecreaseBalance() {
        double qty = 2.0;

        tradeService.buy(user.getUsername(), asset.getExternalId(), qty);

        User refreshed = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(1000.0 - (qty * asset.getCurrentPrice()), refreshed.getBalance(), 0.0001);

        var holdings = userHoldingRepository.findByUserId(user.getId());
        assertEquals(1, holdings.size());
        assertEquals(qty, holdings.get(0).getQuantity(), 0.0001);
    }

    @Test
    void sell_shouldDecreaseHolding_andIncreaseBalance_andDeleteHoldingWhenZero() {
        double qty = 2.0;

        tradeService.buy(user.getUsername(), asset.getExternalId(), qty);

        tradeService.sell(user.getUsername(), asset.getExternalId(), qty);

        User refreshed = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(1000.0, refreshed.getBalance(), 0.0001);

        var holdings = userHoldingRepository.findByUserId(user.getId());
        assertTrue(holdings.isEmpty());
    }

}