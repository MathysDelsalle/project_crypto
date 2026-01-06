package api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Le nom d'utilisateur est requis.")
    @Size(min = 3, max = 30, message = "Le nom d'utilisateur doit faire entre 3 et 30 caractères.")
    private String username;

    @NotBlank(message = "L'email est requis.")
    @Email(message = "Email invalide.")
    @Pattern(
        regexp = "^[^\\s@]+@[^\\s@]+\\.(com|fr)$",
        message = "Email invalide (ex: exemple@site.com ou .fr)."
    )
    private String email;

    @NotBlank(message = "Le mot de passe est requis.")
    @Size(min = 8, message = "Le mot de passe doit faire au moins 8 caractères.")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_\\-+=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$",
        message = "Le mot de passe doit contenir 1 majuscule, 1 chiffre et 1 caractère spécial."
    )
    private String password;
}
