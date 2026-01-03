package collector.dto;


import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class PricePointDto {
    private Long timestamp;
    private Double price;
}
