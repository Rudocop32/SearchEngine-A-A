package searchengine.dto.statistics;

import lombok.Data;

import java.util.List;

@Data
public class StatisticsData {
    private TotalStatistics total;
    private List<DetailedStatisticsItem> detailed;

    public TotalStatistics getTotal() {
        return total;
    }

    public void setTotal(TotalStatistics total) {
        this.total = total;
    }

    public List<DetailedStatisticsItem> getDetailed() {
        return detailed;
    }

    public void setDetailed(List<DetailedStatisticsItem> detailed) {
        this.detailed = detailed;
    }
}
