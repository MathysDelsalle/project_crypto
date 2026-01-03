package api.controller;


import api.model.CryptoAsset;
import api.service.CryptoAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;



@RestController
@RequestMapping("/api/cryptos")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class CryptoAssetController{

    private final CryptoAssetService cryptoAssetService;

    @GetMapping
    public List<CryptoAsset> getAllCrypto() {
        return cryptoAssetService.getAllCrypto();
    }

    @GetMapping("/{externalId}")
    public ResponseEntity<CryptoAsset> getCryptoByExternalId(@PathVariable String externalId) {
        return cryptoAssetService.getByExternalID(externalId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
    }
    
    



}