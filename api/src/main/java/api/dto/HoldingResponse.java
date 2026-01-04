package api.dto;

import lombok.Data;

@Data
public class HoldingResponse {
    private String externalId;
    private String name;
    private String symbol;
    private Double quantity;
}
