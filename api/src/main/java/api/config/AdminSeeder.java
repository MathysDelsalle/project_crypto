package api.config;

import api.model.Role;
import api.model.User;
import api.repository.RoleRepository;
import api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class AdminSeeder {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  @Bean
  CommandLineRunner seedAdmin(
      org.springframework.core.env.Environment env
  ) {
    return args -> {
      String username = env.getProperty("ADMIN_USERNAME");
      String password = env.getProperty("ADMIN_PASSWORD");
      String email = env.getProperty("ADMIN_EMAIL");

      if (username == null || password == null || email == null) return;

      Role adminRole = roleRepository.findByName("ROLE_ADMIN")
          .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").build()));

      Role userRole = roleRepository.findByName("ROLE_USER")
          .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));

      var admin = userRepository.findByUsername(username).orElse(null);
      if (admin == null) {
        admin = User.builder()
            .username(username)
            .email(email)
            .password(passwordEncoder.encode(password))
            .enabled(true)
            .build();
      }

      admin.getRoles().add(userRole);
      admin.getRoles().add(adminRole);

      userRepository.save(admin);
    };
  }
}
