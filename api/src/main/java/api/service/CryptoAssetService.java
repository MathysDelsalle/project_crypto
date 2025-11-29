package api.service;


import api.model.CryptoAsset;
import api.repository.CryptoAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CryptoAssetService{


    // Repository JPA pour récupérer les cryptos
    private final CryptoAssetRepository cryptoAssetRepository;

    public List<CryptoAsset> getAllCrypto(){
        return cryptoAssetRepository.findAll();
    }

    public Optional<CryptoAsset> getByExternalID(String  id){
        return cryptoAssetRepository.findByExternalId(id);
    }




}