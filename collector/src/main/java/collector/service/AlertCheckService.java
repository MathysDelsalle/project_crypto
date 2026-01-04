package collector.service;

import collector.model.PriceAlert;
import collector.repository.PriceAlertRepository;
import collector.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertCheckService {

    private final PriceAlertRepository alertRepository;
    private final UserRepository userRepository;
    private final MailService mailService;

    @Value("${app.mail.frontendUrl:http://localhost:3000}")
    private String frontendUrl;

    @Transactional
    public void checkAlerts() {
        List<PriceAlert> alerts = alertRepository.findByActiveTrue();
        Instant now = Instant.now();

        for (PriceAlert alert : alerts) {

            var asset = alert.getAsset();
            Double price = asset != null ? asset.getCurrentPrice() : null;
            if (price == null) continue;

            // 🔑 récupérer l'email utilisateur
            String email = userRepository.findById(alert.getUserId())
                    .map(u -> u.getEmail())
                    .orElse(null);

            if (email == null) {
                log.warn("⚠️ Aucun email pour userId={}", alert.getUserId());
                continue;
            }

            String assetName = asset.getExternalId();
            String link = frontendUrl + "/alerts";

            // 🔺 ALERTE HIGH
            if (alert.getThresholdHigh() != null
                    && alert.getLastTriggeredHighAt() == null
                    && price >= alert.getThresholdHigh()) {

                String html = buildAlertHtml(
                        assetName, price, alert.getThresholdHigh(),
                        "AU-DESSUS", link
                );

                mailService.sendHtml(
                        email,
                        "🚨 Alerte HIGH " + assetName,
                        html
                );

                alert.setLastTriggeredHighAt(now);

                log.warn("📧 EMAIL HIGH envoyé → {} ({})", email, assetName);
            }

            // 🔻 ALERTE LOW
            if (alert.getThresholdLow() != null
                    && alert.getLastTriggeredLowAt() == null
                    && price <= alert.getThresholdLow()) {

                String html = buildAlertHtml(
                        assetName, price, alert.getThresholdLow(),
                        "EN-DESSOUS", link
                );

                mailService.sendHtml(
                        email,
                        "🚨 Alerte LOW " + assetName,
                        html
                );

                alert.setLastTriggeredLowAt(now);

                log.warn("📧 EMAIL LOW envoyé → {} ({})", email, assetName);
            }
        }
    }

    private String buildAlertHtml(
            String asset, double price,
            double target, String direction,
            String link
    ) {
        return """
            <div style="font-family:Arial,sans-serif;line-height:1.6">
              <h2>🚨 Alerte crypto déclenchée</h2>
              <p><b>Actif :</b> %s</p>
              <p><b>Prix actuel :</b> %.6f</p>
              <p><b>Seuil :</b> %.6f (%s)</p>
              <p>
                <a href="%s"
                   style="display:inline-block;padding:10px 14px;
                          background:#111;color:#fff;
                          text-decoration:none;border-radius:6px">
                  Ouvrir l'application
                </a>
              </p>
              <hr/>
              <p style="color:#666;font-size:12px">
                Email automatique — ne pas répondre
              </p>
            </div>
            """.formatted(asset, price, target, direction, link);
    }
}
