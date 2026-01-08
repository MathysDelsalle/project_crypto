package api.config;

import api.service.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ❌ CSRF inutile en API stateless
            .csrf(csrf -> csrf.disable())

            // ✅ CORS activé
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            .authorizeHttpRequests(auth -> auth
                // ✅ Préflight CORS
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // ✅ Auth publique
                .requestMatchers(HttpMethod.POST,
                        "/api/auth/register",
                        "/api/auth/login"
                ).permitAll()
                .requestMatchers("/api/auth/**", "/error").permitAll()

                // ✅ Endpoints publics
                .requestMatchers(HttpMethod.GET,
                        "/api/cryptos/**",
                        "/api/crypto/**"
                ).permitAll()

                // ✅ Actuator health
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                // 🔒 Admin
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // 🔒 Tout le reste protégé
                .anyRequest().authenticated()
            )

            // ✅ Stateless JWT
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ✅ Autorise localhost + ingress (*.crypto.local)
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://*.crypto.local",
                "https://*.crypto.local"
        ));

        // ✅ Méthodes autorisées
        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // ✅ Headers (important pour éviter des blocages silencieux)
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));


        // ✅ Headers exposés au frontend
        configuration.setExposedHeaders(List.of("Authorization"));

        // ⚠️ Obligatoire si Authorization / cookies
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
