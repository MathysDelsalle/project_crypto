package collector.service;

import collector.model.CryptoAsset;
import collector.model.PriceAlert;
import collector.model.User;
import collector.repository.PriceAlertRepository;
import collector.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertCheckServiceTest {

    @Mock
    private PriceAlertRepository alertRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MailService mailService;

    @InjectMocks
    private AlertCheckService service;

    @Test
    void checkAlerts_whenNoActiveAlerts_doesNothing() {
        when(alertRepository.findByActiveTrue()).thenReturn(List.of());

        service.checkAlerts();

        verifyNoInteractions(userRepository);
        verifyNoInteractions(mailService);
    }

    @Test
    void checkAlerts_whenAssetOrPriceNull_skipsAlert() {
        PriceAlert alert = baseAlert();
        alert.setAsset(null); // asset null => skip

        when(alertRepository.findByActiveTrue()).thenReturn(List.of(alert));

        service.checkAlerts();

        verifyNoInteractions(userRepository);
        verifyNoInteractions(mailService);
    }

    @Test
    void checkAlerts_whenUserEmailNotFound_skips_andDoesNotSendMail() {
        PriceAlert alert = baseAlert();
        when(alertRepository.findByActiveTrue()).thenReturn(List.of(alert));
        when(userRepository.findById(alert.getUserId())).thenReturn(Optional.empty());

        service.checkAlerts();

        verify(userRepository, times(1)).findById(alert.getUserId());
        verifyNoInteractions(mailService);
    }

    @Test
    void checkAlerts_whenHighTriggered_sendsEmail_andSetsLastTriggeredHighAt() {
        // frontendUrl utilisé pour le lien dans le HTML
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:3000");

        PriceAlert alert = baseAlert();
        alert.setThresholdHigh(200.0);
        alert.setLastTriggeredHighAt(null);
        alert.getAsset().setCurrentPrice(250.0);

        when(alertRepository.findByActiveTrue()).thenReturn(List.of(alert));
        when(userRepository.findById(alert.getUserId())).thenReturn(Optional.of(user("test@example.com")));

        service.checkAlerts();

        // ✅ email envoyé
        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);

        verify(mailService, times(1)).sendHtml(toCaptor.capture(), subjectCaptor.capture(), htmlCaptor.capture());

        assertThat(toCaptor.getValue()).isEqualTo("test@example.com");
        assertThat(subjectCaptor.getValue()).isEqualTo("🚨 Alerte HIGH bitcoin"); // assetName = externalId
        assertThat(htmlCaptor.getValue()).contains("AU-DESSUS");
        assertThat(htmlCaptor.getValue()).contains("http://localhost:3000/alerts");

        // ✅ champ mis à jour (pas besoin de save car @Transactional + entité managée)
        assertThat(alert.getLastTriggeredHighAt()).isNotNull();
    }

    @Test
    void checkAlerts_whenLowTriggered_sendsEmail_andSetsLastTriggeredLowAt() {
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:3000");

        PriceAlert alert = baseAlert();
        alert.setThresholdLow(100.0);
        alert.setLastTriggeredLowAt(null);
        alert.getAsset().setCurrentPrice(90.0);

        when(alertRepository.findByActiveTrue()).thenReturn(List.of(alert));
        when(userRepository.findById(alert.getUserId())).thenReturn(Optional.of(user("test@example.com")));

        service.checkAlerts();

        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);

        verify(mailService, times(1)).sendHtml(toCaptor.capture(), subjectCaptor.capture(), htmlCaptor.capture());

        assertThat(toCaptor.getValue()).isEqualTo("test@example.com");
        assertThat(subjectCaptor.getValue()).isEqualTo("🚨 Alerte LOW bitcoin");
        assertThat(htmlCaptor.getValue()).contains("EN-DESSOUS");
        assertThat(htmlCaptor.getValue()).contains("http://localhost:3000/alerts");

        assertThat(alert.getLastTriggeredLowAt()).isNotNull();
    }

    @Test
    void checkAlerts_whenAlreadyTriggeredHigh_doesNotSendAgain() {
        PriceAlert alert = baseAlert();
        alert.setThresholdHigh(200.0);
        alert.setLastTriggeredHighAt(Instant.now().minusSeconds(60));
        alert.getAsset().setCurrentPrice(250.0);

        when(alertRepository.findByActiveTrue()).thenReturn(List.of(alert));
        when(userRepository.findById(alert.getUserId())).thenReturn(Optional.of(user("test@example.com")));

        service.checkAlerts();

        verifyNoInteractions(mailService);
    }

    @Test
    void checkAlerts_whenAlreadyTriggeredLow_doesNotSendAgain() {
        PriceAlert alert = baseAlert();
        alert.setThresholdLow(100.0);
        alert.setLastTriggeredLowAt(Instant.now().minusSeconds(60));
        alert.getAsset().setCurrentPrice(90.0);

        when(alertRepository.findByActiveTrue()).thenReturn(List.of(alert));
        when(userRepository.findById(alert.getUserId())).thenReturn(Optional.of(user("test@example.com")));

        service.checkAlerts();

        verifyNoInteractions(mailService);
    }

    @Test
    void checkAlerts_whenBothThresholdsMet_andBothNeverTriggered_sendsTwoEmails_andSetsBothDates() {
        // Cas “edge” : si thresholdLow == thresholdHigh et price == seuil,
        // les deux if peuvent passer (<= low et >= high).
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:3000");

        PriceAlert alert = baseAlert();
        alert.setThresholdLow(100.0);
        alert.setThresholdHigh(100.0);
        alert.getAsset().setCurrentPrice(100.0);
        alert.setLastTriggeredLowAt(null);
        alert.setLastTriggeredHighAt(null);

        when(alertRepository.findByActiveTrue()).thenReturn(List.of(alert));
        when(userRepository.findById(alert.getUserId())).thenReturn(Optional.of(user("test@example.com")));

        service.checkAlerts();

        verify(mailService, times(2)).sendHtml(anyString(), anyString(), anyString());
        assertThat(alert.getLastTriggeredLowAt()).isNotNull();
        assertThat(alert.getLastTriggeredHighAt()).isNotNull();
    }

    // --------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------

    private static PriceAlert baseAlert() {
        CryptoAsset asset = new CryptoAsset();
        asset.setExternalId("bitcoin");
        asset.setCurrentPrice(150.0);

        PriceAlert alert = new PriceAlert();
        alert.setActive(true);
        alert.setUserId(10L);     // IMPORTANT: ton code lit getUserId()
        alert.setAsset(asset);

        // seuils par défaut
        alert.setThresholdLow(100.0);
        alert.setThresholdHigh(200.0);

        return alert;
    }

    private static User user(String email) {
        User u = new User();
        u.setEmail(email);
        return u;
    }
}
