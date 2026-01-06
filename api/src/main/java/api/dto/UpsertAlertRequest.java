package api.dto;

import lombok.Data;

@Data
public class UpsertAlertRequest {

    // ex: "bitcoin"
    private String externalId;

    private Double thresholdHigh; // nullable
    private Double thresholdLow;  // nullable

    private Boolean active; // nullable (default true)
}
