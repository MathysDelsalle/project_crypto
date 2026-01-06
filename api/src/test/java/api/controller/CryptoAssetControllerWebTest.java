package api.controller;

import api.config.SecurityConfig;
import api.model.CryptoAsset;
import api.service.CryptoAssetService;
import api.service.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CryptoAssetController.class)
@Import(SecurityConfig.class)
class CryptoAssetControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CryptoAssetService cryptoAssetService;

    // ✅ requis par SecurityConfig
    @MockBean
    private JwtAuthenticationFilter jwtAuthFilter;

    @MockBean
    private AuthenticationProvider authenticationProvider;

    @BeforeEach
    void setup() throws Exception {
        // IMPORTANT: un @MockBean de Filter fait "rien" par défaut => ça bloque la chaîne.
        // On le transforme en no-op qui laisse passer.
        doAnswer((Answer<Void>) invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    @Test
    void getAllCrypto_returns200_andJsonArray() throws Exception {
        CryptoAsset btc = CryptoAsset.builder()
                .externalId("btc")
                .name("Bitcoin")
                .symbol("BTC")
                .build();

        CryptoAsset eth = CryptoAsset.builder()
                .externalId("eth")
                .name("Ethereum")
                .symbol("ETH")
                .build();

        when(cryptoAssetService.getAllCrypto()).thenReturn(List.of(btc, eth));

        mockMvc.perform(get("/api/cryptos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].externalId").value("btc"))
                .andExpect(jsonPath("$[0].name").value("Bitcoin"))
                .andExpect(jsonPath("$[0].symbol").value("BTC"))
                .andExpect(jsonPath("$[1].externalId").value("eth"))
                .andExpect(jsonPath("$[1].name").value("Ethereum"))
                .andExpect(jsonPath("$[1].symbol").value("ETH"));
    }

    @Test
    void getCryptoByExternalId_found_returns200() throws Exception {
        CryptoAsset btc = CryptoAsset.builder()
                .externalId("btc")
                .name("Bitcoin")
                .symbol("BTC")
                .build();

        when(cryptoAssetService.getByExternalID("btc")).thenReturn(Optional.of(btc));

        mockMvc.perform(get("/api/cryptos/btc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalId").value("btc"))
                .andExpect(jsonPath("$.name").value("Bitcoin"))
                .andExpect(jsonPath("$.symbol").value("BTC"));
    }

    @Test
    void getCryptoByExternalId_notFound_returns404() throws Exception {
        when(cryptoAssetService.getByExternalID("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/cryptos/unknown"))
                .andExpect(status().isNotFound());
    }
}
