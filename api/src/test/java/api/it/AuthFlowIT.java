package api.it;

import api.model.Role;
import api.repository.RoleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIT extends AbstractPostgresIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private RoleRepository roleRepository;

    @BeforeEach
    void ensureRoleExists() {
        roleRepository.findByName("ROLE_USER").orElseGet(() -> {
            Role r = new Role();
            r.setName("ROLE_USER");
            return roleRepository.save(r);
        });
    }

    @Test
    void register_then_login_then_accessProtectedEndpoint_withJwt() throws Exception {
        // 1) register
        String registerJson = """
        {
          "username": "mathys_it",
          "email": "mathys_it@test.com",
          "password": "Password1!"
        }
        """;

        String registerResp = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        String tokenAfterRegister = objectMapper.readTree(registerResp).get("token").asText();

        // token du register marche déjà
        mockMvc.perform(get("/api/test/protected/username")
                        .header("Authorization", "Bearer " + tokenAfterRegister))
                .andExpect(status().isOk())
                .andExpect(content().string("mathys_it"));

        // 2) login
        String loginJson = """
        {
          "username": "mathys_it",
          "password": "Password1!"
        }
        """;

        String loginResp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(loginResp).get("token").asText();

        // 3) endpoint protégé -> OK
        mockMvc.perform(get("/api/test/protected/username")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("mathys_it"));

        // 4) sans token -> 403 FORBIDDEN
        mockMvc.perform(get("/api/test/protected/username"))
                .andExpect(status().isForbidden());

    }

    @TestConfiguration
    static class TestOnlyControllerConfig {
        @Bean
        TestProtectedController testProtectedController() {
            return new TestProtectedController();
        }
    }

    @RestController
    static class TestProtectedController {
        @GetMapping("/api/test/protected/username")
        public String whoAmI(Authentication auth) {
            return auth.getName();
        }
    }
}
