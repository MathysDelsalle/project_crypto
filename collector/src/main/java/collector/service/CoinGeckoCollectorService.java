package collector.service;

import collector.dto.CoinGeckoCoinDto;
import collector.model.CryptoAsset;
import collector.repository.CryptoAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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

    /**
     * Récupère les cryptos depuis CoinGecko et les insère / met à jour en base.
     */
    public void collectTopMarketCoins() {

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

        List<CoinGeckoCoinDto> coins = Arrays.asList(response);
        log.info("✅ {} cryptos récupérées depuis CoinGecko.", coins.size());

        for (CoinGeckoCoinDto coin : coins) {
            try {
                // On cherche d'abord si la crypto existe déjà (par symbol ou id CoinGecko)
                CryptoAsset asset = cryptoAssetRepository
                        .findByExternalId(coin.getId())
                        .orElseGet(CryptoAsset::new);

                if (asset.getId() == null) {
                    // Nouveau record
                    asset.setExternalId(coin.getId());
                }

                asset.setSymbol(coin.getSymbol());
                asset.setName(coin.getName());
                asset.setCurrentPrice(coin.getCurrent_price());
                asset.setMarketCap(coin.getMarket_cap());
                asset.setTotalVolume(coin.getTotal_volume());
                asset.setPrice_change_24h(coin.getPrice_change_24h());
                asset.setImageUrl(coin.getImage());

                cryptoAssetRepository.save(asset);
            } catch (Exception e) {
                log.error("❌ Erreur lors de la sauvegarde de la crypto {} ({})",
                        coin.getName(), coin.getId(), e);
            }
        }

        log.info("🏁 Collecte CoinGecko terminée, cryptos enregistrées / mises à jour en BDD.");
    }
}
