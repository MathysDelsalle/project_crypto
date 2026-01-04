package collector.service;

import collector.model.CryptoAsset;
import collector.repository.CryptoAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import collector.repository.CryptoPriceHistoryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoinGeckoTop100HistoryBootstrapService {

    private final CryptoAssetRepository assetRepository;
    private final CryptoPriceHistoryRepository historyRepository;
    private final CoinGeckoHistoryCollectorService historyCollectorService;

    /**
     * Bootstrap 7 jours UNIQUEMENT pour les assets qui n'ont pas encore d'historique.
     * Retourne le nombre de cryptos bootstrappées dans ce run.
     */
    public int bootstrapMissingTop100(String vsCurrency, int batchSize) {

        List<CryptoAsset> top100 = assetRepository.findTop100ByOrderByMarketCapRankAsc();
        if (top100 == null || top100.isEmpty()) {
            log.warn("Top100 indisponible (crypto_assets vide ?) -> rien à bootstraper.");
            return 0;
        }

        int done = 0;

        for (CryptoAsset asset : top100) {
            if (done >= batchSize) break;
            if (asset.getId() == null || asset.getExternalId() == null) continue;

            // ✅ check BDD: a-t-il déjà au moins un point d'historique ?
            if (historyRepository.existsByAssetIdAndVsCurrency(asset.getId(), vsCurrency)) {
                continue;
            }

            try {
                log.info("Bootstrap 7j manquant: {} ({})", asset.getName(), asset.getExternalId());
                historyCollectorService.fillLast7Days(asset.getExternalId(), vsCurrency);
                done++;

                // anti-429 (CoinGecko free)
                Thread.sleep(1500);

            } catch (WebClientResponseException.TooManyRequests e) {
                log.warn("429 CoinGecko. On reprendra au prochain tick.");
                break;

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;

            } catch (Exception e) {
                log.error("Erreur bootstrap 7j pour {} ({})", asset.getName(), asset.getExternalId(), e);
            }
        }

        return done;
    }
}
