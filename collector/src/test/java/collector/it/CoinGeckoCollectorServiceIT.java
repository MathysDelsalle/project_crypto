package collector.it;

import collector.repository.CryptoAssetRepository;
import collector.repository.CryptoPriceHistoryRepository;
import collector.service.CoinGeckoCollectorService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CoinGeckoCollectorServiceIT {

    @MockBean
    JavaMailSender javaMailSender;

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    static MockWebServer mockWebServer;

    @BeforeAll
    static void setup() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);

        r.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        r.add("spring.sql.init.mode", () -> "never");

        // WebClient baseUrl → MockWebServer
        r.add("coingecko.api.url", () -> mockWebServer.url("/api/v3").toString());
    }

    @Autowired CoinGeckoCollectorService collectorService;
    @Autowired CryptoAssetRepository assetRepository;
    @Autowired CryptoPriceHistoryRepository historyRepository;

    @Test
    void collectTopMarketCoins_persists_assets_and_writes_now_history() {
        String json = """
            [
              {
                "id":"bitcoin",
                "symbol":"btc",
                "name":"Bitcoin",
                "current_price":50000.0,
                "market_cap":1000000.0,
                "total_volume":2000.0,
                "price_change_24h":10.0,
                "image":"http://img",
                "market_cap_rank":1
              }
            ]
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(json)
        );

        collectorService.collectTopMarketCoins(true);

        assertThat(assetRepository.count()).isEqualTo(1);
        assertThat(historyRepository.count()).isEqualTo(1);
    }
}
