package api.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundsRequest {
    private Double delta; // +100 = ajouter, -50 = enlever
}
