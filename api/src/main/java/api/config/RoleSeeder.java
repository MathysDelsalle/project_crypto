package api.config;

import api.model.Role;
import api.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RoleSeeder {

  private final RoleRepository roleRepository;

  @Bean
  CommandLineRunner seedRoles() {
    return args -> {
      roleRepository.findByName("ROLE_USER")
          .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));

      roleRepository.findByName("ROLE_ADMIN")
          .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").build()));
    };
  }
}

