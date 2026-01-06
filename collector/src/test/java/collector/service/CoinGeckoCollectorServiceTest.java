package collector.service;


import collector.model.CryptoAsset;
import collector.repository.CryptoAssetRepository;
import collector.repository.CryptoPriceHistoryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CoinGeckoCollectorServiceTest {

    private static WebClient webClientReturningJson(String json) {
        ExchangeFunction exchange = request -> Mono.just(
            ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .build()
        );
        return WebClient.builder().exchangeFunction(exchange).build();
    }

    private static WebClient webClientReturningStatus(HttpStatus status, String body) {
        ExchangeFunction exchange = request -> Mono.just(
            ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build()
        );
        return WebClient.builder().exchangeFunction(exchange).build();
    }

    @Test
    void collectTopMarketCoins_whenAssetAlreadyExists_updatesIt_and_upsertsHistory() {
        String json = """
            [
            {
                "id":"bitcoin",
                "symbol":"btc",
                "name":"Bitcoin",
                "current_price":60000.0,
                "market_cap":2000000.0,
                "total_volume":3000.0,
                "image":"http://img2",
                "market_cap_rank":1
            }
            ]
            """;

        WebClient wc = webClientReturningJson(json);

        CryptoAssetRepository assetRepo = mock(CryptoAssetRepository.class);
        CryptoPriceHistoryRepository historyRepo = mock(CryptoPriceHistoryRepository.class);

        // 🔹 Asset déjà présent en base
        CryptoAsset existing = new CryptoAsset();
        existing.setId(42L);
        existing.setExternalId("bitcoin");
        existing.setSymbol("btc");
        existing.setName("Bitcoin");
        existing.setCurrentPrice(50000.0); // ancienne valeur

        when(assetRepo.findByExternalId("bitcoin"))
            .thenReturn(Optional.of(existing));

        when(assetRepo.save(any(CryptoAsset.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        CoinGeckoCollectorService service =
            new CoinGeckoCollectorService(wc, assetRepo, historyRepo);

        // ▶️ exécution
        service.collectTopMarketCoins(true);

        // 🔍 capture de l'asset sauvegardé
        ArgumentCaptor<CryptoAsset> captor =
            ArgumentCaptor.forClass(CryptoAsset.class);

        verify(assetRepo).save(captor.capture());

        CryptoAsset saved = captor.getValue();

        // ✅ même ID → update, pas création
        assertThat(saved.getId()).isEqualTo(42L);
        assertThat(saved.getExternalId()).isEqualTo("bitcoin");

        // ✅ champs mis à jour
        assertThat(saved.getCurrentPrice()).isEqualTo(60000.0);
        assertThat(saved.getMarketCap()).isEqualTo(2000000.0);
        assertThat(saved.getTotalVolume()).isEqualTo(3000.0);
        assertThat(saved.getImageUrl()).isEqualTo("http://img2");
        assertThat(saved.getMarketCapRank()).isEqualTo(1);

        // ✅ historique écrit avec l'id existant
        verify(historyRepo).upsertPoint(
            eq(42L),
            eq("usd"),
            any(),
            eq(60000.0),
            eq(2000000.0),
            eq(3000.0)
        );
    }



    @Test
    void collectTopMarketCoins_writeHistory_false_savesAsset_but_doesNotWriteHistory() {
        String json = """
            [
              {"id":"bitcoin","symbol":"btc","name":"Bitcoin","current_price":50000.0}
            ]
            """;

        WebClient wc = webClientReturningJson(json);

        CryptoAssetRepository assetRepo = mock(CryptoAssetRepository.class);
        CryptoPriceHistoryRepository historyRepo = mock(CryptoPriceHistoryRepository.class);

        when(assetRepo.findByExternalId("bitcoin")).thenReturn(Optional.empty());
        when(assetRepo.save(any(CryptoAsset.class))).thenAnswer(inv -> {
            CryptoAsset a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        CoinGeckoCollectorService service =
            new CoinGeckoCollectorService(wc, assetRepo, historyRepo);

        service.collectTopMarketCoins(false);

        verify(assetRepo, times(1)).save(any());
        verify(historyRepo, never()).upsertPoint(any(), any(), any(), any(), any(), any());
    }

    @Test
    void collectTopMarketCoins_emptyResponse_doesNothing() {
        WebClient wc = webClientReturningJson("[]");

        CryptoAssetRepository assetRepo = mock(CryptoAssetRepository.class);
        CryptoPriceHistoryRepository historyRepo = mock(CryptoPriceHistoryRepository.class);

        CoinGeckoCollectorService service =
            new CoinGeckoCollectorService(wc, assetRepo, historyRepo);

        service.collectTopMarketCoins(true);

        verifyNoInteractions(assetRepo);
        verifyNoInteractions(historyRepo);
    }

    @Test
    void collectTopMarketCoins_whenAssetDoesNotExist_and_writeHistory_true_createsAsset_and_upsertsHistory() {
        String json = """
            [
            {
                "id":"bitcoin",
                "symbol":"btc",
                "name":"Bitcoin",
                "current_price":50000.0,
                "market_cap":1000000.0,
                "total_volume":2000.0,
                "image":"http://img",
                "market_cap_rank":1
            }
            ]
            """;

        WebClient wc = webClientReturningJson(json);

        CryptoAssetRepository assetRepo = mock(CryptoAssetRepository.class);
        CryptoPriceHistoryRepository historyRepo = mock(CryptoPriceHistoryRepository.class);

        when(assetRepo.findByExternalId("bitcoin")).thenReturn(Optional.empty());

        // Simule génération ID par la DB
        when(assetRepo.save(any(CryptoAsset.class))).thenAnswer(inv -> {
            CryptoAsset a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        CoinGeckoCollectorService service =
            new CoinGeckoCollectorService(wc, assetRepo, historyRepo);

        service.collectTopMarketCoins(true);

        ArgumentCaptor<CryptoAsset> captor = ArgumentCaptor.forClass(CryptoAsset.class);
        verify(assetRepo).save(captor.capture());

        CryptoAsset saved = captor.getValue();
        assertThat(saved.getExternalId()).isEqualTo("bitcoin");
        assertThat(saved.getSymbol()).isEqualTo("btc");
        assertThat(saved.getName()).isEqualTo("Bitcoin");
        assertThat(saved.getCurrentPrice()).isEqualTo(50000.0);
        assertThat(saved.getMarketCap()).isEqualTo(1000000.0);
        assertThat(saved.getTotalVolume()).isEqualTo(2000.0);
        assertThat(saved.getImageUrl()).isEqualTo("http://img");
        assertThat(saved.getMarketCapRank()).isEqualTo(1);

        verify(historyRepo).upsertPoint(
            eq(1L),
            eq("usd"),
            any(),
            eq(50000.0),
            eq(1000000.0),
            eq(2000.0)
        );
    }

    @Test
    void collectTopMarketCoins_whenCoinGeckoReturns429_throwsTooManyRequests_and_doesNotTouchRepos() {
        WebClient wc = webClientReturningStatus(
            HttpStatus.TOO_MANY_REQUESTS,
            """
            {"status":{"error_code":429,"error_message":"Too Many Requests"}}
            """
        );

        CryptoAssetRepository assetRepo = mock(CryptoAssetRepository.class);
        CryptoPriceHistoryRepository historyRepo = mock(CryptoPriceHistoryRepository.class);

        CoinGeckoCollectorService service =
            new CoinGeckoCollectorService(wc, assetRepo, historyRepo);

        assertThatThrownBy(() -> service.collectTopMarketCoins(true))
            .isInstanceOf(WebClientResponseException.TooManyRequests.class);

        verifyNoInteractions(assetRepo);
        verifyNoInteractions(historyRepo);
    }

    @Test
    void collectTopMarketCoins_callsCoinGeckoMarketsEndpoint_withExpectedQueryParams() {
        // JSON minimal non vide pour ne pas sortir tôt
        String json = """
            [
            {"id":"bitcoin","symbol":"btc","name":"Bitcoin","current_price":50000.0}
            ]
            """;

        // Capture de l'URI réellement appelée
        java.util.concurrent.atomic.AtomicReference<java.net.URI> calledUri =
            new java.util.concurrent.atomic.AtomicReference<>();

        ExchangeFunction exchange = request -> {
            calledUri.set(request.url()); // <-- on garde l'URL appelée
            return Mono.just(
                ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .build()
            );
        };

        WebClient wc = WebClient.builder().exchangeFunction(exchange).build();

        CryptoAssetRepository assetRepo = mock(CryptoAssetRepository.class);
        CryptoPriceHistoryRepository historyRepo = mock(CryptoPriceHistoryRepository.class);

        when(assetRepo.findByExternalId("bitcoin")).thenReturn(Optional.empty());
        when(assetRepo.save(any(CryptoAsset.class))).thenAnswer(inv -> {
            CryptoAsset a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        CoinGeckoCollectorService service =
            new CoinGeckoCollectorService(wc, assetRepo, historyRepo);

        // writeNowHistory = false pour ne pas dépendre de Instant.now() / upsert
        service.collectTopMarketCoins(false);

        // ✅ vérifie l'URL appelée
        java.net.URI uri = calledUri.get();
        assertThat(uri).as("Request URI should be captured").isNotNull();
        assertThat(uri.getPath()).endsWith("/coins/markets");

        var params = UriComponentsBuilder.fromUri(uri).build().getQueryParams();

        assertThat(params.getFirst("vs_currency")).isEqualTo("usd");
        assertThat(params.getFirst("order")).isEqualTo("market_cap_desc");
        assertThat(params.getFirst("per_page")).isEqualTo("100");
        assertThat(params.getFirst("page")).isEqualTo("1");
        assertThat(params.getFirst("sparkline")).isEqualTo("false");
    }

}
