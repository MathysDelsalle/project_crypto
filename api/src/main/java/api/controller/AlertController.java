package api.controller;

import api.dto.AlertResponse;
import api.dto.UpsertAlertRequest;
import api.model.CryptoAsset;
import api.model.PriceAlert;
import api.model.User;
import api.repository.CryptoAssetRepository;
import api.repository.PriceAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/me/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final PriceAlertRepository alertRepository;
    private final CryptoAssetRepository assetRepository;

    // ==========================
    // GET : récupérer mes alertes
    // ==========================
    @GetMapping
    public List<AlertResponse> getMyAlerts(@AuthenticationPrincipal User user) {
        return alertRepository.findByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ==========================
    // POST : créer ou modifier (upsert)
    // ==========================
    @PostMapping
    public ResponseEntity<AlertResponse> upsertAlertPost(
            @AuthenticationPrincipal User user,
            @RequestBody UpsertAlertRequest request
    ) {
        return upsertInternal(user, request);
    }

    // ==========================
    // PUT : créer ou modifier (upsert)
    // ==========================
    @PutMapping
    public ResponseEntity<AlertResponse> upsertAlertPut(
            @AuthenticationPrincipal User user,
            @RequestBody UpsertAlertRequest request
    ) {
        return upsertInternal(user, request);
    }

    @DeleteMapping("/{externalId}")
    public ResponseEntity<Void> deleteAlert(
            @AuthenticationPrincipal User user,
            @PathVariable String externalId
    ) {
        CryptoAsset asset = assetRepository.findByExternalId(externalId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown crypto: " + externalId));

        long deleted = alertRepository.deleteByUserIdAndAsset_Id(user.getId(), asset.getId());
        return deleted > 0 ? ResponseEntity.noContent().build()
                        : ResponseEntity.notFound().build();
    }



    // ==========================
    // LOGIQUE COMMUNE (1 SEULE FOIS)
    // ==========================
    private ResponseEntity<AlertResponse> upsertInternal(
            User user,
            UpsertAlertRequest request
    ) {
        if (request.getExternalId() == null || request.getExternalId().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        if (request.getThresholdHigh() == null && request.getThresholdLow() == null) {
            return ResponseEntity.badRequest().build();
        }

        CryptoAsset asset = assetRepository.findByExternalId(request.getExternalId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown crypto: " + request.getExternalId()));

        PriceAlert alert = alertRepository
                .findByUserIdAndAsset_Id(user.getId(), asset.getId())
                .orElseGet(PriceAlert::new);

        alert.setUserId(user.getId());
        alert.setAsset(asset);
        alert.setThresholdHigh(request.getThresholdHigh());
        alert.setThresholdLow(request.getThresholdLow());
        alert.setActive(request.getActive() == null || request.getActive());

        // reset triggers à chaque modification
        alert.setLastTriggeredHighAt(null);
        alert.setLastTriggeredLowAt(null);

        PriceAlert saved = alertRepository.save(alert);
        return ResponseEntity.ok(toResponse(saved));
    }

    // ==========================
    // Mapper réponse
    // ==========================
    private AlertResponse toResponse(PriceAlert alert) {
        AlertResponse r = new AlertResponse();
        r.setExternalId(alert.getAsset().getExternalId());
        r.setName(alert.getAsset().getName());
        r.setSymbol(alert.getAsset().getSymbol());
        r.setThresholdHigh(alert.getThresholdHigh());
        r.setThresholdLow(alert.getThresholdLow());
        r.setActive(alert.isActive());
        return r;
    }
}
