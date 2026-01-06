package api.model;

import java.io.Serializable;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFavoriteId implements Serializable {
    private Long userId;
    private Long assetId;
}
