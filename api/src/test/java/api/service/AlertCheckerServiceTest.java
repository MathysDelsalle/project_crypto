package api.service;

import api.model.CryptoAsset;
import api.model.PriceAlert;
import api.repository.PriceAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AlertCheckerServiceTest {

    private PriceAlertRepository alertRepository;
    private AlertCheckerService service;

    @BeforeEach
    void setup() {
        alertRepository = mock(PriceAlertRepository.class);
        service = new AlertCheckerService(alertRepository);
    }

    @Test
    void checkAlerts_doesNothingWhenNoActiveAlerts() {
        when(alertRepository.findByActiveTrue()).thenReturn(List.of());

        service.checkAlerts();

        verify(alertRepository, never()).save(any());
    }

    @Test
    void checkAlerts_skipsWhenPriceNull() {
        CryptoAsset asset = new CryptoAsset();
        asset.setCurrentPrice(null);

        PriceAlert alert = new PriceAlert();
        alert.setAsset(asset);

        when(alertRepository.findByActiveTrue()).thenReturn(List.of(alert));

        service.checkAlerts();

        verify(alertRepository, never()).save(any());
    }

    @Test
    void checkAlerts_triggersHighOnce_andSaves() {
        CryptoAsset asset = new CryptoAsset();
        asset.setName("Bitcoin");
        asset.setExternalId("btc");
        asset.setCurrentPrice(120.0);

        PriceAlert alert = new PriceAlert();
        alert.setUserId(1L);
        alert.setAsset(asset);
        alert.setThresholdHigh(100.0);
        alert.setLastTriggeredHighAt(null);

        when(alertRepository.findByActiveTrue()).thenReturn(List.of(alert));

        service.checkAlerts();

        ArgumentCaptor<PriceAlert> captor = ArgumentCaptor.forClass(PriceAlert.class);
        verify(alertRepository).save(captor.capture());
        assertNotNull(captor.getValue().getLastTriggeredHighAt());
    }

    @Test
    void checkAlerts_doesNotTriggerHighTwice() {
        CryptoAsset asset = new CryptoAsset();
        asset.setCurrentPrice(120.0);

        PriceAlert alert = new PriceAlert();
        alert.setAsset(asset);
        alert.setThresholdHigh(100.0);
        alert.setLastTriggeredHighAt(Instant.now()); // déjà déclenchée

        when(alertRepository.findByActiveTrue()).thenReturn(List.of(alert));

        service.checkAlerts();

        verify(alertRepository, never()).save(any());
    }

    @Test
    void checkAlerts_triggersLowOnce_andSaves() {
        CryptoAsset asset = new CryptoAsset();
        asset.setCurrentPrice(80.0);

        PriceAlert alert = new PriceAlert();
        alert.setUserId(1L);
        alert.setAsset(asset);
        alert.setThresholdLow(90.0);
        alert.setLastTriggeredLowAt(null);

        when(alertRepository.findByActiveTrue()).thenReturn(List.of(alert));

        service.checkAlerts();

        verify(alertRepository).save(alert);
        assertNotNull(alert.getLastTriggeredLowAt());
    }

    @Test
    void checkAlerts_triggersBothHighAndLow_ifBothThresholdsAndNeverTriggered() {
        CryptoAsset asset = new CryptoAsset();
        asset.setCurrentPrice(100.0);

        PriceAlert alert = new PriceAlert();
        alert.setUserId(1L);
        alert.setAsset(asset);
        alert.setThresholdHigh(100.0);
        alert.setThresholdLow(100.0);

        when(alertRepository.findByActiveTrue()).thenReturn(List.of(alert));

        service.checkAlerts();

        verify(alertRepository).save(alert);
        assertNotNull(alert.getLastTriggeredHighAt());
        assertNotNull(alert.getLastTriggeredLowAt());
    }
}
