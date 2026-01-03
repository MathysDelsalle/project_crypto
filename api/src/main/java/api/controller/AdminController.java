package api.controller;

import api.model.Role;
import api.repository.RoleRepository;
import api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping("/promote/{username}")
  public String promoteToAdmin(@PathVariable String username) {
    var user = userRepository.findByUsername(username)
        .orElseThrow(() -> new RuntimeException("User not found"));

    Role adminRole = roleRepository.findByName("ROLE_ADMIN")
        .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));

    user.getRoles().add(adminRole);
    userRepository.save(user);

    return "OK: " + username + " is now ADMIN";
  }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/demote/{username}")
    public String demoteToUser(@PathVariable String username) {

    var user = userRepository.findByUsername(username)
        .orElseThrow(() -> new RuntimeException("User not found"));

    Role adminRole = roleRepository.findByName("ROLE_ADMIN")
        .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));

    Role userRole = roleRepository.findByName("ROLE_USER")
        .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));

    // Sécurité : empêcher un admin de se retirer lui-même
    if (user.getUsername().equals(
        org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication().getName()
    )) {
        throw new RuntimeException("You cannot demote yourself");
    }

    // Retire ADMIN
    user.getRoles().remove(adminRole);

    // S’assure que USER est présent
    user.getRoles().add(userRole);

    userRepository.save(user);

    return "OK: " + username + " is now USER";
    }

}
