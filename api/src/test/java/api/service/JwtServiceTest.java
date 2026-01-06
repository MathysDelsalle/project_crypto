package api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setup() {
        jwtService = new JwtService();

        // secret en Base64 (doit faire >= 256 bits pour HS256)
        byte[] secretBytes = "0123456789ABCDEF0123456789ABCDEF".getBytes(StandardCharsets.UTF_8);
        String base64Secret = Base64.getEncoder().encodeToString(secretBytes);

        ReflectionTestUtils.setField(jwtService, "secretKey", base64Secret);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 60_000L); // 1 minute
    }

    @Test
    void generateToken_andExtractUsername_ok() {
        UserDetails user = User.withUsername("mathys")
                .password("x")
                .authorities("ROLE_USER")
                .build();

        String token = jwtService.generateToken(user);
        assertNotNull(token);

        String username = jwtService.extractUsername(token);
        assertEquals("mathys", username);

        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void tokenInvalid_whenUsernameDoesNotMatch() {
        UserDetails user1 = User.withUsername("mathys")
                .password("x")
                .authorities("ROLE_USER")
                .build();

        UserDetails user2 = User.withUsername("other")
                .password("x")
                .authorities("ROLE_USER")
                .build();

        String token = jwtService.generateToken(user1);

        assertFalse(jwtService.isTokenValid(token, user2));
    }

    @Test
    void tokenExpired_throwsExpiredJwtException() throws Exception {
        // expiration très courte
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 1L);

        UserDetails user = User.withUsername("mathys")
                .password("x")
                .authorities("ROLE_USER")
                .build();

        String token = jwtService.generateToken(user);

        Thread.sleep(5);

        assertThrows(io.jsonwebtoken.ExpiredJwtException.class,
                () -> jwtService.isTokenValid(token, user));
    }


    @Test
    void tamperedToken_throwsWhenExtractingUsername() {
        UserDetails user = User.withUsername("mathys")
                .password("x")
                .authorities("ROLE_USER")
                .build();

        String token = jwtService.generateToken(user);

        // on altère le token (signature invalide)
        String tampered = token.substring(0, token.length() - 1) + "x";

        assertThrows(Exception.class, () -> jwtService.extractUsername(tampered));
        assertThrows(Exception.class, () -> jwtService.isTokenValid(tampered, user));
    }

    @Test
    void generateToken_withExtraClaims_stillValid_andSubjectOk() {
        UserDetails user = User.withUsername("mathys")
                .password("x")
                .authorities("ROLE_USER")
                .build();

        String token = jwtService.generateToken(Map.of("foo", "bar"), user);

        assertEquals("mathys", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, user));
    }
}
