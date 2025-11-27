package collector.scheduler;

import collector.service.CoinGeckoCollectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CoinGeckoScheduler {

    private final CoinGeckoCollectorService collectorService;

    /**
     * Tâche planifiée qui lance la collecte périodiquement.
     *
     * @schedule (fixedRate = x): permet de définir un scheduler interne a Spring qui execute la commande tout les x ms 
     * 
     */
    @Scheduled(fixedRate = 30_000)// toutes les 15 secondes 
    public void runCollection() {
        log.info("Collecte CoinGecko démarrée par le scheduler.");
        try {
            collectorService.collectTopMarketCoins();
        } catch (Exception e) {
            log.error("Erreur lors de l'exécution de la tâche de collecte CoinGecko", e);
        }
    }
}
