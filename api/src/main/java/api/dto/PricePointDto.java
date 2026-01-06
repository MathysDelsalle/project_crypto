package api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PricePointDto {
    private long ts;      // epoch millis
    private double price;
}
