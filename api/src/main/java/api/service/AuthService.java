package api.service;

import api.dto.AuthResponse;
import api.dto.LoginRequest;
import api.dto.RegisterRequest;
import api.exception.BusinessException;
import api.model.Role;
import api.model.User;
import api.repository.RoleRepository;
import api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        // Erreurs attendues => BusinessException (pas de stacktrace, status propre)
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Ce nom d'utilisateur est déjà pris.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Cet email est déjà utilisé.");
        }

        var user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .build();

        // Problème de config serveur => 500 (on garde stacktrace via handler générique si tu préfères)
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "ROLE_USER not found"));

        user.getRoles().add(userRole);

        userRepository.save(user);

        var jwtToken = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(jwtToken)
                .username(user.getUsername())
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        String identifier = request.getUsername();

        // Erreur attendue => 401 (évite d’indiquer si user existe ou pas)
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Identifiants invalides."));

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(), // username réel attendu par Spring Security
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException ex) {
            // Erreur attendue => 401, pas de stacktrace
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Identifiants invalides.");
        }

        var jwtToken = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(jwtToken)
                .username(user.getUsername())
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .build();
    }
}
