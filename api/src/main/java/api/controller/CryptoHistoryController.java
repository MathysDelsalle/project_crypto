package api.controller;

import api.dto.PricePointDto;
import api.service.PriceHistoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/crypto")
public class CryptoHistoryController {

    private final PriceHistoryQueryService historyService;

    // Ex: GET /api/crypto/bitcoin/history?vs=usd
    @GetMapping("/{externalId}/history")
    public List<PricePointDto> getHistory7Days(
            @PathVariable String externalId,
            @RequestParam(name = "vs", defaultValue = "usd") String vsCurrency
    ) {
        return historyService.getLast7DaysSeriesByExternalId(externalId, vsCurrency);
    }
}
