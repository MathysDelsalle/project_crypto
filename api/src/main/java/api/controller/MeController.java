package api.controller;

import api.service.UserSpaceService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me")
public class MeController {

    private final UserSpaceService userSpaceService;

    @GetMapping("/balance")
    public Map<String, Double> getBalance(Authentication auth) {
        String username = auth.getName();
        return Map.of("balance", userSpaceService.getBalance(username));
    }

    @PostMapping("/balance/add")
    public Map<String, Double> addBalance(Authentication auth, @RequestBody AddBalanceRequest req) {
        String username = auth.getName();
        double newBalance = userSpaceService.addBalance(username, req.getAmount());
        return Map.of("balance", newBalance);
    }

    @GetMapping("/favorites")
    public List<String> getFavorites(Authentication auth) {
        String username = auth.getName();
        return userSpaceService.getFavorites(username);
    }

    @PostMapping("/favorites/{externalId}")
    public void addFavorite(Authentication auth, @PathVariable String externalId) {
        String username = auth.getName();
        userSpaceService.addFavorite(username, externalId);
    }

    @DeleteMapping("/favorites/{externalId}")
    public void removeFavorite(Authentication auth, @PathVariable String externalId) {
        String username = auth.getName();
        userSpaceService.removeFavorite(username, externalId);
    }

    @Data
    public static class AddBalanceRequest {
        private double amount;
    }
}
