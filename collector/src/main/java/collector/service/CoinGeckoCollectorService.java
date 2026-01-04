package collector.service;

import collector.dto.CoinGeckoCoinDto;
import collector.model.CryptoAsset;
import collector.repository.CryptoAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import collector.repository.CryptoPriceHistoryRepository;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoinGeckoCollectorService {

    // Injecté depuis WebClientConfig
    private final WebClient coinGeckoWebClient;

    // Repository JPA pour enregistrer / mettre à jour les cryptos
    private final CryptoAssetRepository cryptoAssetRepository;

    // Historique
    private final CryptoPriceHistoryRepository cryptoPriceHistoryRepository;

    /**
     * Comportement actuel (inchangé) :
     * - met à jour crypto_assets
     * - + écrit un point NOW dans crypto_price_history
     */
    public void collectTopMarketCoins() {
        collectTopMarketCoins(true);
    }

    /**
     * Variante interne:
     * writeNowHistory=false => utile pour le bootstrap (remplit crypto_assets sans écrire NOW)
     */
    public void collectTopMarketCoins(boolean writeNowHistory) {

        Mono<CoinGeckoCoinDto[]> monoResponse = coinGeckoWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/coins/markets")
                        .queryParam("vs_currency", "usd")
                        .queryParam("order", "market_cap_desc")
                        .queryParam("per_page", "100")
                        .queryParam("page", "1")
                        .queryParam("sparkline", "false")
                        .build()
                )
                .retrieve()
                .bodyToMono(CoinGeckoCoinDto[].class);

        CoinGeckoCoinDto[] response = monoResponse.block(); // OK dans un batch/scheduler

        if (response == null || response.length == 0) {
            log.warn("⚠️ Réponse CoinGecko vide, aucune donnée collectée.");
            return;
        }

        // Timestamp commun au run si on écrit NOW
        Instant now = writeNowHistory
                ? Instant.now().truncatedTo(ChronoUnit.MINUTES)
                : null;

        String vsCurrency = "usd";

        List<CoinGeckoCoinDto> coins = Arrays.asList(response);
        log.info("✅ {} cryptos récupérées depuis CoinGecko.", coins.size());

        for (CoinGeckoCoinDto coin : coins) {
            try {
                CryptoAsset asset = cryptoAssetRepository
                        .findByExternalId(coin.getId())
                        .orElseGet(CryptoAsset::new);

                if (asset.getId() == null) {
                    asset.setExternalId(coin.getId());
                }

                asset.setSymbol(coin.getSymbol());
                asset.setName(coin.getName());
                asset.setCurrentPrice(coin.getCurrent_price());
                asset.setMarketCap(coin.getMarket_cap());
                asset.setTotalVolume(coin.getTotal_volume());
                asset.setPrice_change_24h(coin.getPrice_change_24h());
                asset.setImageUrl(coin.getImage());
                asset.setMarketCapRank(coin.getMarket_cap_rank());

                CryptoAsset saved = cryptoAssetRepository.save(asset);

                // ✅ On écrit NOW seulement si demandé (phase normale)
                if (writeNowHistory && saved.getId() != null && saved.getCurrentPrice() != null) {
                    cryptoPriceHistoryRepository.upsertPoint(
                            saved.getId(),
                            vsCurrency,
                            now,
                            saved.getCurrentPrice(),
                            saved.getMarketCap(),
                            saved.getTotalVolume()
                    );
                }

            } catch (Exception e) {
                log.error("❌ Erreur lors de la sauvegarde de la crypto {} ({})",
                        coin.getName(), coin.getId(), e);
            }
        }

        log.info("🏁 Collecte CoinGecko terminée, cryptos enregistrées / mises à jour en BDD.");
    }
}
