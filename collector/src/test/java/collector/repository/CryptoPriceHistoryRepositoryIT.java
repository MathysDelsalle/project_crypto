package collector.repository;

import collector.model.CryptoAsset;
import collector.model.CryptoPriceHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CryptoPriceHistoryRepositoryIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);

        // On veut que Hibernate crée le schéma depuis les @Entity
        r.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        // Évite que Spring essaie de rejouer init.sql
        r.add("spring.sql.init.mode", () -> "never");
    }

    @Autowired CryptoAssetRepository assetRepository;
    @Autowired CryptoPriceHistoryRepository historyRepository;

    @Test
    void upsertPoint_insertsThenUpdates_sameUniqueKey() {
        CryptoAsset asset = new CryptoAsset();
        asset.setExternalId("bitcoin");
        asset.setSymbol("btc");
        asset.setName("Bitcoin");
        asset = assetRepository.save(asset);

        Instant ts = Instant.parse("2024-01-01T00:00:00Z");

        historyRepository.upsertPoint(asset.getId(), "usd", ts, 10.0, 100.0, 1000.0);
        historyRepository.upsertPoint(asset.getId(), "usd", ts, 11.0, 110.0, 1100.0); // update via ON CONFLICT

        List<CryptoPriceHistory> all = historyRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getPrice()).isEqualTo(11.0);
        assertThat(all.get(0).getMarketCap()).isEqualTo(110.0);
        assertThat(all.get(0).getTotalVolume()).isEqualTo(1100.0);
    }
}
