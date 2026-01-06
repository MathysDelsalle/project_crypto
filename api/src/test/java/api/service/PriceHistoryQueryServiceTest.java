package api.service;

import api.dto.PricePointDto;
import api.model.CryptoAsset;
import api.repository.CryptoAssetRepository;
import api.repository.CryptoPriceHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PriceHistoryQueryServiceTest {

    private CryptoAssetRepository assetRepository;
    private CryptoPriceHistoryRepository historyRepository;

    private PriceHistoryQueryService service;

    @BeforeEach
    void setup() {
        assetRepository = mock(CryptoAssetRepository.class);
        historyRepository = mock(CryptoPriceHistoryRepository.class);
        service = new PriceHistoryQueryService(assetRepository, historyRepository);
    }

    @Test
    void getLast7Days_returnsEmptyWhenAssetNotFound() {
        when(assetRepository.findByExternalId("btc")).thenReturn(Optional.empty());

        List<PricePointDto> out = service.getLast7DaysSeriesByExternalId("btc", "usd");

        assertTrue(out.isEmpty());
        verifyNoInteractions(historyRepository);
    }

    @Test
    void getLast7Days_convertsTimestampTypes() {
        CryptoAsset asset = new CryptoAsset();
        asset.setId(10L);

        when(assetRepository.findByExternalId("btc")).thenReturn(Optional.of(asset));

        Instant i = Instant.parse("2025-01-01T00:00:00Z");
        Timestamp ts = Timestamp.from(i);
        OffsetDateTime odt = OffsetDateTime.ofInstant(i, ZoneOffset.UTC);
        LocalDateTime ldt = LocalDateTime.ofInstant(i, ZoneOffset.UTC);

        when(historyRepository.findPriceSeries(eq(10L), eq("usd"), any(Instant.class)))
                .thenReturn(List.of(
                        new Object[]{ts, 100.0},
                        new Object[]{odt, 101.0},
                        new Object[]{ldt, 102.0},
                        new Object[]{i, 103.0}
                ));

        List<PricePointDto> out = service.getLast7DaysSeriesByExternalId("btc", "usd");

        assertEquals(4, out.size());

        assertEquals(i.toEpochMilli(), out.get(0).getTs());
        assertEquals(100.0, out.get(0).getPrice());

        assertEquals(i.toEpochMilli(), out.get(1).getTs());
        assertEquals(101.0, out.get(1).getPrice());

        assertEquals(i.toEpochMilli(), out.get(2).getTs());
        assertEquals(102.0, out.get(2).getPrice());

        assertEquals(i.toEpochMilli(), out.get(3).getTs());
        assertEquals(103.0, out.get(3).getPrice());

    }

    @Test
    void getLast7Days_throwsWhenUnexpectedTsType() {
        CryptoAsset asset = new CryptoAsset();
        asset.setId(10L);

        when(assetRepository.findByExternalId("btc")).thenReturn(Optional.of(asset));
        when(historyRepository.findPriceSeries(eq(10L), eq("usd"), any(Instant.class)))
                .thenReturn(List.<Object[]>of(new Object[]{new Object(), 100.0}));


        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.getLast7DaysSeriesByExternalId("btc", "usd"));

        assertTrue(ex.getMessage().startsWith("Type ts inattendu:"));
    }
}
