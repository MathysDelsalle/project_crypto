package api.service;

import api.model.PriceAlert;
import api.repository.PriceAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertCheckerService {

    private final PriceAlertRepository alertRepository;

    @Scheduled(fixedDelay = 60_000) // toutes les 60 secondes
    public void checkAlerts() {
        List<PriceAlert> alerts = alertRepository.findByActiveTrue();
        if (alerts.isEmpty()) return;

        for (PriceAlert alert : alerts) {
            Double price = alert.getAsset().getCurrentPrice();
            if (price == null) continue;

            boolean updated = false;

            if (alert.getThresholdHigh() != null
                    && alert.getLastTriggeredHighAt() == null
                    && price >= alert.getThresholdHigh()) {

                log.warn(
                    "[ALERTE] User {} | {} ({}) | seuil HAUT atteint : {} >= {}",
                    alert.getUserId(),
                    alert.getAsset().getName(),
                    alert.getAsset().getExternalId(),
                    price,
                    alert.getThresholdHigh()
                );

                alert.setLastTriggeredHighAt(Instant.now());
                updated = true;
            }

            if (alert.getThresholdLow() != null
                    && alert.getLastTriggeredLowAt() == null
                    && price <= alert.getThresholdLow()) {

                log.warn(
                    "[ALERTE] User {} | {} ({}) | seuil BAS atteint : {} <= {}",
                    alert.getUserId(),
                    alert.getAsset().getName(),
                    alert.getAsset().getExternalId(),
                    price,
                    alert.getThresholdLow()
                );

                alert.setLastTriggeredLowAt(Instant.now());
                updated = true;
            }

            if (updated) {
                alertRepository.save(alert);
            }
        }
    }
}
