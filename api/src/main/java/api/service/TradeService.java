package api.service;

import api.dto.HoldingResponse;
import api.model.CryptoAsset;
import api.model.User;
import api.model.UserHolding;
import api.repository.CryptoAssetRepository;
import api.repository.UserHoldingRepository;
import api.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final UserRepository userRepository;
    private final CryptoAssetRepository assetRepository;
    private final UserHoldingRepository holdingRepository;

    public List<HoldingResponse> getHoldings(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return holdingRepository.findByUserId(user.getId()).stream().map(h -> {
            HoldingResponse r = new HoldingResponse();
            r.setExternalId(h.getAsset().getExternalId());
            r.setName(h.getAsset().getName());
            r.setSymbol(h.getAsset().getSymbol());
            r.setQuantity(h.getQuantity());
            return r;
        }).toList();
    }

    @Transactional
    public double buy(String username, String externalId, double qty) {
        if (qty <= 0) throw new IllegalArgumentException("qty must be > 0");

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        CryptoAsset asset = assetRepository.findByExternalId(externalId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown crypto: " + externalId));

        double price = asset.getCurrentPrice() == null ? 0.0 : asset.getCurrentPrice();
        if (price <= 0) throw new IllegalArgumentException("Price unavailable");

        double cost = price * qty;
        if (user.getBalance() < cost) throw new IllegalArgumentException("Solde insuffisant");

        // balance
        user.setBalance(user.getBalance() - cost);
        userRepository.save(user);

        // holding
        UserHolding holding = holdingRepository
                .findByUserIdAndAsset_Id(user.getId(), asset.getId())
                .orElseGet(() -> {
                    UserHolding h = new UserHolding();
                    h.setUserId(user.getId());
                    h.setAsset(asset);
                    h.setQuantity(0.0);
                    return h;
                });

        holding.setQuantity(holding.getQuantity() + qty);
        holdingRepository.save(holding);

        return user.getBalance();
    }

    @Transactional
    public double sell(String username, String externalId, double qty) {
        if (qty <= 0) throw new IllegalArgumentException("qty must be > 0");

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        CryptoAsset asset = assetRepository.findByExternalId(externalId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown crypto: " + externalId));

        double price = asset.getCurrentPrice() == null ? 0.0 : asset.getCurrentPrice();
        if (price <= 0) throw new IllegalArgumentException("Price unavailable");

        UserHolding holding = holdingRepository
                .findByUserIdAndAsset_Id(user.getId(), asset.getId())
                .orElseThrow(() -> new IllegalArgumentException("Aucune possession pour cette crypto"));

        if (holding.getQuantity() < qty) throw new IllegalArgumentException("Quantité insuffisante");

        // update holding
        holding.setQuantity(holding.getQuantity() - qty);
        if (holding.getQuantity() <= 0) {
            holdingRepository.delete(holding);
        } else {
            holdingRepository.save(holding);
        }

        // credit balance
        double gain = price * qty;
        user.setBalance(user.getBalance() + gain);
        userRepository.save(user);

        return user.getBalance();
    }
}
