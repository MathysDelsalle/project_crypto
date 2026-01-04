package collector.scheduler;

import collector.service.AlertCheckService;
import collector.service.CoinGeckoCollectorService;
import collector.service.CoinGeckoTop100HistoryBootstrapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import collector.repository.CryptoPriceHistoryRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class CoinGeckoScheduler {

    private final CoinGeckoCollectorService collectorService;
    private final CryptoPriceHistoryRepository historyRepository;
    private final CoinGeckoTop100HistoryBootstrapService bootstrapService;

    // ✅ NEW
    private final AlertCheckService alertCheckService;

    private volatile boolean top100LoadedOnce = false;

    @Scheduled(fixedRate = 30_000)
    public void runCollection() {
        String vsCurrency = "usd";

        try {
            // 1) Assure que crypto_assets est rempli (Top100) au moins 1 fois
            if (!top100LoadedOnce) {
                collectorService.collectTopMarketCoins(false);
                top100LoadedOnce = true;
            }

            // 2) Bootstrap uniquement les cryptos manquantes
            long bootstrapped = historyRepository.countDistinctAssetsByVsCurrency(vsCurrency);

            if (bootstrapped < 100) {
                int batchSize = 2;
                int done = bootstrapService.bootstrapMissingTop100(vsCurrency, batchSize);
                long after = historyRepository.countDistinctAssetsByVsCurrency(vsCurrency);

                log.info("Bootstrap (manquants) : +{} ce tick | {}/100 ont un historique.", done, after);
                return;
            }

            // 3) Mode normal : update prix + NOW
            collectorService.collectTopMarketCoins(true);
            log.info("Mode normal: collecte + point NOW OK.");

            // ✅ 4) Check alertes après mise à jour des prix
            alertCheckService.checkAlerts();

        } catch (WebClientResponseException.TooManyRequests e) {
            log.warn("429 Too Many Requests. On réessaiera au prochain tick. {}", e.getMessage());
        } catch (Exception e) {
            log.error("Erreur scheduler CoinGecko", e);
        }
    }
}
