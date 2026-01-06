package api.it;

import api.model.CryptoAsset;
import api.model.PriceAlert;
import api.model.Role;
import api.model.User;
import api.repository.CryptoAssetRepository;
import api.repository.PriceAlertRepository;
import api.repository.RoleRepository;
import api.repository.UserRepository;
import api.service.AlertCheckerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AlertCheckerServiceIT extends AbstractPostgresIT {

    @Autowired private AlertCheckerService alertCheckerService;
    @Autowired private PriceAlertRepository priceAlertRepository;
    @Autowired private CryptoAssetRepository cryptoAssetRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;

    private User user;
    private CryptoAsset asset;

    @BeforeEach
    void setup() {
        // Nettoyage dans un ordre "FK-safe"
        priceAlertRepository.deleteAll();
        userRepository.deleteAll();
        cryptoAssetRepository.deleteAll();
        // ⚠️ On ne supprime PAS les roles : c'est une table de référence partagée entre tests

        Role roleUser = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setName("ROLE_USER");
                    return roleRepository.save(r);
                });

        String username = "alert_it_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        user = User.builder()
                .username(username)
                .email(username + "@test.com")
                .password("encoded")
                .enabled(true)
                .balance(0.0)
                .build();
        user.getRoles().add(roleUser);
        user = userRepository.save(user);

        asset = CryptoAsset.builder()
                .externalId("btc")
                .name("Bitcoin")
                .symbol("BTC")
                .currentPrice(120.0)
                .build();
        asset = cryptoAssetRepository.save(asset);
    }

    @Test
    void checkAlerts_shouldSetTriggeredHighTimestamp_whenThresholdReached() {
        PriceAlert alert = new PriceAlert();
        alert.setUserId(user.getId());
        alert.setAsset(asset);
        alert.setActive(true);

        alert.setThresholdHigh(100.0);
        alert.setThresholdLow(null);

        alert.setLastTriggeredHighAt(null);
        alert.setLastTriggeredLowAt(null);

        alert = priceAlertRepository.save(alert);

        alertCheckerService.checkAlerts();

        PriceAlert refreshed = priceAlertRepository.findById(alert.getId()).orElseThrow();
        assertNotNull(refreshed.getLastTriggeredHighAt(), "Le seuil haut devrait être marqué comme déclenché");
        assertNull(refreshed.getLastTriggeredLowAt());

        // sanity : timestamp raisonnable
        assertTrue(refreshed.getLastTriggeredHighAt().isBefore(Instant.now().plusSeconds(2)));
    }
}
