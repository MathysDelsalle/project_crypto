package api.service;

import api.dto.PricePointDto;
import api.repository.CryptoAssetRepository;
import api.repository.CryptoPriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceHistoryQueryService {

    private final CryptoAssetRepository assetRepository;
    private final CryptoPriceHistoryRepository historyRepository;

    public List<PricePointDto> getLast7DaysSeriesByExternalId(String externalId, String vsCurrency) {
        var assetOpt = assetRepository.findByExternalId(externalId);
            if (assetOpt.isEmpty()) {
                return List.of(); // historique vide → pas d'erreur HTTP
            }
            var asset = assetOpt.get();


        Instant from = Instant.now().minus(7, ChronoUnit.DAYS);

        List<Object[]> rows = historyRepository.findPriceSeries(asset.getId(), vsCurrency, from);

        List<PricePointDto> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            // r[0] = ts (Timestamp), r[1] = price (Double)
            Object tsObj = r[0];
            Instant ts;

            if (tsObj instanceof java.sql.Timestamp t) {
                ts = t.toInstant();
            } else if (tsObj instanceof java.time.OffsetDateTime odt) {
                ts = odt.toInstant();
            } else if (tsObj instanceof java.time.LocalDateTime ldt) {
                ts = ldt.atZone(java.time.ZoneOffset.UTC).toInstant();
            } else if (tsObj instanceof Instant i) {
                ts = i;
            } else {
                throw new IllegalStateException("Type ts inattendu: " + tsObj.getClass());
            }

            double price = ((Number) r[1]).doubleValue();
            out.add(new PricePointDto(ts.toEpochMilli(), price));

        }
        return out;
    }
}
