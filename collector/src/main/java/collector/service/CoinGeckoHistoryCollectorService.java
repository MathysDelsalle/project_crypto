package collector.service;

import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import collector.dto.CoinGeckoMarketChartDto;
import collector.repository.CryptoAssetRepository;
import collector.repository.CryptoPriceHistoryRepository;

@Service
@RequiredArgsConstructor
public class CoinGeckoHistoryCollectorService {

    private final WebClient webClient;
    private final CryptoAssetRepository cryptoAssetRepository;
    private final CryptoPriceHistoryRepository historyRepository;

    /**
     * Remplissage initial : récupère les données de prix sur 7 jours.
     */
    public int fillLast7Days(String coinGeckoId, String vsCurrency) {

        var asset = cryptoAssetRepository.findByExternalId(coinGeckoId)
            .orElseThrow(() ->
                new IllegalArgumentException("Asset introuvable: external_id=" + coinGeckoId)
            );

        CoinGeckoMarketChartDto dto = webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/coins/{id}/market_chart")
                .queryParam("vs_currency", vsCurrency)
                .queryParam("days", 7)
                .build(coinGeckoId)
            )
            .retrieve()
            .bodyToMono(CoinGeckoMarketChartDto.class)
            .block();

        if (dto == null || dto.getPrices() == null) {
            return 0;
        }

        List<List<Double>> prices = dto.getPrices();
        List<List<Double>> marketCaps = dto.getMarketCaps();
        List<List<Double>> volumes = dto.getTotalVolumes();

        int count = 0;

        for (int i = 0; i < prices.size(); i++) {
            List<Double> p = prices.get(i);

            Instant ts = Instant.ofEpochMilli(p.get(0).longValue());
            Double price = p.get(1);

            Double mc = (marketCaps != null && marketCaps.size() > i)
                ? marketCaps.get(i).get(1)
                : null;

            Double vol = (volumes != null && volumes.size() > i)
                ? volumes.get(i).get(1)
                : null;

            historyRepository.upsertPoint(
                asset.getId(),
                vsCurrency,
                ts,
                price,
                mc,
                vol
            );

            count++;
        }

        return count;
    }
}
