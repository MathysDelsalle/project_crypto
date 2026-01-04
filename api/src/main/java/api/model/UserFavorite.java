package api.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "user_favorites")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFavorite {

    @EmbeddedId
    private UserFavoriteId id;

    // optionnel si tu as created_at en DB
    @Column(name = "created_at")
    private Instant createdAt;
}
