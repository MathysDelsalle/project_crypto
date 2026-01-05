package collector.service;

import collector.model.CryptoAsset;
import collector.repository.CryptoAssetRepository;
import collector.repository.CryptoPriceHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class CoinGeckoHistoryCollectorServiceTest {

    private static WebClient webClientReturningJson(String json) {
        ExchangeFunction exchange = request -> Mono.just(
            ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .build()
        );
        return WebClient.builder().exchangeFunction(exchange).build();
    }

    @Test
    void fillLast7Days_insertsAllPoints_and_returnsCount() {
        String json = """
          {
            "prices":[
              [1700000000000, 10.0],
              [1700000060000, 11.0]
            ],
            "market_caps":[
              [1700000000000, 100.0],
              [1700000060000, 110.0]
            ],
            "total_volumes":[
              [1700000000000, 1000.0],
              [1700000060000, 1100.0]
            ]
          }
          """;

        WebClient wc = webClientReturningJson(json);

        CryptoAssetRepository assetRepo = mock(CryptoAssetRepository.class);
        CryptoPriceHistoryRepository historyRepo = mock(CryptoPriceHistoryRepository.class);

        CryptoAsset asset = new CryptoAsset();
        asset.setId(42L);
        asset.setExternalId("bitcoin");

        when(assetRepo.findByExternalId("bitcoin")).thenReturn(Optional.of(asset));

        CoinGeckoHistoryCollectorService service =
            new CoinGeckoHistoryCollectorService(wc, assetRepo, historyRepo);

        int inserted = service.fillLast7Days("bitcoin", "usd");

        assertThat(inserted).isEqualTo(2);

        verify(historyRepo, times(2)).upsertPoint(
            eq(42L),
            eq("usd"),
            any(),
            anyDouble(),
            anyDouble(),
            anyDouble()
        );
    }
}
