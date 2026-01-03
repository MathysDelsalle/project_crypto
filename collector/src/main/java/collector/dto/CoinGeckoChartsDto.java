package collector.dto;

import lombok.Data;
import java.util.List;

@Data
public class CoinGeckoChartsDto {

    private List<List<Double>> prices;
    private List<List<Double>> market_caps;
    private List<List<Double>> total_volumes;
}
