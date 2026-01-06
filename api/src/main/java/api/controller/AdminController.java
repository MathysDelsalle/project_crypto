package api.controller;

import api.dto.AdminUserDto;
import api.dto.FundsRequest;
import api.model.Role;
import api.repository.RoleRepository;
import api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/users")
  public List<AdminUserDto> listUsers() {
    return userRepository.findAll().stream()
        .map(u -> AdminUserDto.builder()
            .id(u.getId())
            .username(u.getUsername())
            .email(u.getEmail())
            .balance(u.getBalance())
            .enabled(u.isEnabled())
            .roles(u.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
            .build()
        )
        .collect(Collectors.toList());
  }

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

    // empêcher un admin de se retirer lui-même
    var currentUsername = org.springframework.security.core.context.SecurityContextHolder
        .getContext().getAuthentication().getName();

    if (user.getUsername().equals(currentUsername)) {
      throw new RuntimeException("You cannot demote yourself");
    }

    user.getRoles().remove(adminRole);
    user.getRoles().add(userRole);

    userRepository.save(user);

    return "OK: " + username + " is now USER";
  }

  // ✅ Ajouter/enlever des fonds
  @PreAuthorize("hasRole('ADMIN')")
  @PatchMapping("/funds/{username}")
  public AdminUserDto updateFunds(@PathVariable String username, @RequestBody FundsRequest request) {
    if (request == null || request.getDelta() == null) {
      throw new RuntimeException("Missing delta");
    }

    var user = userRepository.findByUsername(username)
        .orElseThrow(() -> new RuntimeException("User not found"));

    double newBalance = (user.getBalance() == null ? 0.0 : user.getBalance()) + request.getDelta();
    if (newBalance < 0) {
      throw new RuntimeException("Balance cannot be negative");
    }

    user.setBalance(newBalance);
    userRepository.save(user);

    return AdminUserDto.builder()
        .id(user.getId())
        .username(user.getUsername())
        .email(user.getEmail())
        .balance(user.getBalance())
        .enabled(user.isEnabled())
        .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
        .build();
  }
}
