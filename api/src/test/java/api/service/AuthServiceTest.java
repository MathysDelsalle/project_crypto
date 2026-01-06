package api.service;

import api.dto.AuthResponse;
import api.dto.LoginRequest;
import api.dto.RegisterRequest;
import api.exception.BusinessException;
import api.model.Role;
import api.model.User;
import api.repository.RoleRepository;
import api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks AuthService authService;

    @Captor ArgumentCaptor<User> userCaptor;

    @Test
    void register_success_createsUser_assignsRole_andReturnsToken() {
        RegisterRequest req = RegisterRequest.builder()
                .username("mathys")
                .email("mathys@test.com")
                .password("Password1!")
                .build();

        when(userRepository.existsByUsername("mathys")).thenReturn(false);
        when(userRepository.existsByEmail("mathys@test.com")).thenReturn(false);

        when(passwordEncoder.encode("Password1!")).thenReturn("ENC(Password1!)");

        Role roleUser = mock(Role.class);
        when(roleUser.getName()).thenReturn("ROLE_USER");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(roleUser));

        when(jwtService.generateToken(any(User.class))).thenReturn("jwt.token");

        AuthResponse res = authService.register(req);

        assertNotNull(res);
        assertEquals("jwt.token", res.getToken());
        assertEquals("mathys", res.getUsername());
        assertTrue(res.getRoles().contains("ROLE_USER"));

        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertEquals("mathys", saved.getUsername());
        assertEquals("mathys@test.com", saved.getEmail());
        assertEquals("ENC(Password1!)", saved.getPassword());
        assertTrue(saved.isEnabled());

        // On vérifie que le rôle a bien été ajouté au user sauvegardé
        assertNotNull(saved.getRoles());
        assertTrue(saved.getRoles().contains(roleUser));

        verify(jwtService).generateToken(any(User.class));
    }

    @Test
    void register_usernameAlreadyTaken_throwsBusinessException409() {
        RegisterRequest req = RegisterRequest.builder()
                .username("mathys")
                .email("mathys@test.com")
                .password("Password1!")
                .build();

        when(userRepository.existsByUsername("mathys")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.register(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("Ce nom d'utilisateur est déjà pris.", ex.getMessage());

        verify(userRepository, never()).save(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void register_emailAlreadyUsed_throwsBusinessException409() {
        RegisterRequest req = RegisterRequest.builder()
                .username("mathys")
                .email("mathys@test.com")
                .password("Password1!")
                .build();

        when(userRepository.existsByUsername("mathys")).thenReturn(false);
        when(userRepository.existsByEmail("mathys@test.com")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.register(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("Cet email est déjà utilisé.", ex.getMessage());

        verify(userRepository, never()).save(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void register_roleUserMissing_throwsBusinessException500() {
        RegisterRequest req = RegisterRequest.builder()
                .username("mathys")
                .email("mathys@test.com")
                .password("Password1!")
                .build();

        when(userRepository.existsByUsername("mathys")).thenReturn(false);
        when(userRepository.existsByEmail("mathys@test.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("ENC");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.register(req));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        assertEquals("ROLE_USER not found", ex.getMessage());

        verify(userRepository, never()).save(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_success_byUsername_authenticates_andReturnsToken() {
        LoginRequest req = LoginRequest.builder()
                .username("mathys")
                .password("Password1!")
                .build();

        User user = mock(User.class);
        when(user.getUsername()).thenReturn("mathys");
        when(userRepository.findByUsername("mathys")).thenReturn(Optional.of(user));

        when(jwtService.generateToken(user)).thenReturn("jwt.token");

        AuthResponse res = authService.login(req);

        assertEquals("jwt.token", res.getToken());
        assertEquals("mathys", res.getUsername());

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);

        verify(authenticationManager).authenticate(captor.capture());
        UsernamePasswordAuthenticationToken passedToken = captor.getValue();
        assertEquals("mathys", passedToken.getPrincipal());
        assertEquals("Password1!", passedToken.getCredentials());

        verify(jwtService).generateToken(user);
    }

    @Test
    void login_success_byEmail_authenticatesUsingRealUsername_andReturnsToken() {
        LoginRequest req = LoginRequest.builder()
                .username("mathys@test.com") // email utilisé comme identifiant
                .password("Password1!")
                .build();

        when(userRepository.findByUsername("mathys@test.com")).thenReturn(Optional.empty());

        User user = mock(User.class);
        when(user.getUsername()).thenReturn("mathys"); // username réel
        when(userRepository.findByEmail("mathys@test.com")).thenReturn(Optional.of(user));

        when(jwtService.generateToken(user)).thenReturn("jwt.token");

        AuthResponse res = authService.login(req);

        assertEquals("jwt.token", res.getToken());
        assertEquals("mathys", res.getUsername());

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);

        verify(authenticationManager).authenticate(captor.capture());
        UsernamePasswordAuthenticationToken passedToken = captor.getValue();
        assertEquals("mathys", passedToken.getPrincipal()); // important
        assertEquals("Password1!", passedToken.getCredentials());
    }

    @Test
    void login_userNotFound_throwsBusinessException401() {
        LoginRequest req = LoginRequest.builder()
                .username("unknown")
                .password("Password1!")
                .build();

        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("unknown")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(req));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("Identifiants invalides.", ex.getMessage());

        verify(authenticationManager, never()).authenticate(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_badCredentials_throwsBusinessException401() {
        LoginRequest req = LoginRequest.builder()
                .username("mathys")
                .password("WrongPassword1!")
                .build();

        User user = mock(User.class);
        when(user.getUsername()).thenReturn("mathys");
        when(userRepository.findByUsername("mathys")).thenReturn(Optional.of(user));

        doThrow(new BadCredentialsException("bad")).when(authenticationManager).authenticate(any());

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(req));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("Identifiants invalides.", ex.getMessage());

        verify(jwtService, never()).generateToken(any());
    }
}
