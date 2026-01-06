package api.service;

import api.model.CryptoAsset;
import api.repository.CryptoAssetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CryptoAssetServiceTest {

    @Mock
    private CryptoAssetRepository cryptoAssetRepository;

    @InjectMocks
    private CryptoAssetService cryptoAssetService;

    @Test
    void getAllCrypto_returnsRepositoryList() {
        CryptoAsset a1 = new CryptoAsset();
        CryptoAsset a2 = new CryptoAsset();
        when(cryptoAssetRepository.findAll()).thenReturn(List.of(a1, a2));

        List<CryptoAsset> out = cryptoAssetService.getAllCrypto();

        assertEquals(2, out.size());
        verify(cryptoAssetRepository).findAll();
        verifyNoMoreInteractions(cryptoAssetRepository);
    }

    @Test
    void getByExternalID_returnsOptionalFromRepository() {
        CryptoAsset asset = new CryptoAsset();
        when(cryptoAssetRepository.findByExternalId("btc")).thenReturn(Optional.of(asset));

        Optional<CryptoAsset> out = cryptoAssetService.getByExternalID("btc");

        assertTrue(out.isPresent());
        verify(cryptoAssetRepository).findByExternalId("btc");
        verifyNoMoreInteractions(cryptoAssetRepository);
    }
}
