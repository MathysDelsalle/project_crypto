package api.dto;

import lombok.Data;

@Data
public class AlertResponse {

    private String externalId;
    private String name;
    private String symbol;

    private Double thresholdHigh;
    private Double thresholdLow;

    private boolean active;
}
