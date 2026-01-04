package api.controller;

import api.dto.HoldingResponse;
import api.service.TradeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me")
public class TradeController {

    private final TradeService tradeService;

    @GetMapping("/holdings")
    public List<HoldingResponse> getHoldings(Authentication auth) {
        return tradeService.getHoldings(auth.getName());
    }

    @PostMapping("/trade/buy/{externalId}")
    public Map<String, Double> buy(Authentication auth, @PathVariable String externalId, @RequestBody QtyRequest req) {
        double newBalance = tradeService.buy(auth.getName(), externalId, req.getQty());
        return Map.of("balance", newBalance);
    }

    @PostMapping("/trade/sell/{externalId}")
    public Map<String, Double> sell(Authentication auth, @PathVariable String externalId, @RequestBody QtyRequest req) {
        double newBalance = tradeService.sell(auth.getName(), externalId, req.getQty());
        return Map.of("balance", newBalance);
    }

    @Data
    public static class QtyRequest {
        private double qty = 1.0; // default 1
    }
}
