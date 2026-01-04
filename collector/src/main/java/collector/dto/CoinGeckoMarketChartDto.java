package collector.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CoinGeckoMarketChartDto {

    @JsonProperty("prices")
    private List<List<Double>> prices;

    @JsonProperty("market_caps")
    private List<List<Double>> marketCaps;

    @JsonProperty("total_volumes")
    private List<List<Double>> totalVolumes;
}
