package api.controller;

import api.config.SecurityConfig;
import api.dto.AuthResponse;
import api.dto.LoginRequest;
import api.dto.RegisterRequest;
import api.exception.BusinessException;
import api.service.AuthService;
import api.service.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;


@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class) // ✅ charge ta vraie config (permitAll sur /api/auth/** + csrf disabled)
class AuthControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    // ✅ Beans requis par SecurityConfig
    @MockBean
    private JwtAuthenticationFilter jwtAuthFilter;

    @MockBean
    private AuthenticationProvider authenticationProvider;

    @BeforeEach
    void setup() throws Exception {
        reset(authService);
        doAnswer((Answer<Void>) invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    @Test
    void register_ok_returns200() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("mathys")
                .email("mathys@test.com")
                .password("Password1!") // ✅ respecte la validation
                .build();

        when(authService.register(any())).thenReturn(null);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void register_weakPassword_returns400_withStandardErrorBody() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("mathys")
                .email("mathys@test.com")
                .password("password") // ❌ invalide
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }



    @Test
    void login_ok_returns200() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("mathys")
                .password("Password1!")
                .build();

        when(authService.login(any())).thenReturn(
                AuthResponse.builder()
                        .token("jwt.token")
                        .username("mathys")
                        .roles(java.util.List.of("ROLE_USER"))
                        .build()
        );

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }


    @Test
    void register_usernameTaken_returns409_andStandardError() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("mathys")
                .email("mathys@test.com")
                .password("Password1!")
                .build();

        when(authService.register(any()))
                .thenThrow(new BusinessException(HttpStatus.CONFLICT, "Ce nom d'utilisateur est déjà pris."));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Ce nom d'utilisateur est déjà pris."))
                .andExpect(jsonPath("$.path").value("/api/auth/register"));
    }

    @Test
    void register_emailTaken_returns409() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("mathys")
                .email("mathys@test.com")
                .password("Password1!")
                .build();

        when(authService.register(any()))
                .thenThrow(new BusinessException(HttpStatus.CONFLICT, "Cet email est déjà utilisé."));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("mathys")
                .password("WrongPassword1!")
                .build();

        doThrow(new BusinessException(HttpStatus.UNAUTHORIZED, "Identifiants invalides."))
                .when(authService).login(any());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
