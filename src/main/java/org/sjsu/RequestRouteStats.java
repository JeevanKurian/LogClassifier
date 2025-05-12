package org.sjsu;

// Import for Apache Commons Math Percentile
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import java.util.ArrayList;
import java.util.Collections; // Keep for sorting for min/max
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RequestRouteStats {
    private final List<Integer> responseTimes = new ArrayList<>();
    private final List<Integer> statusCodes = new ArrayList<>();

    void addResponseTime(int time) {
        this.responseTimes.add(time);
    }

    void addStatusCode(int code) {
        this.statusCodes.add(code);
    }

    // Calculate Response Time statistics
    public Map<String, Object> getResponseTimeStats() {
        Map<String, Object> stats = new HashMap<>();
        if (responseTimes.isEmpty()) {
            stats.put("min", 0);
            stats.put("50_percentile", 0);
            stats.put("90_percentile", 0);
            stats.put("95_percentile", 0);
            stats.put("99_percentile", 0);
            stats.put("max", 0);
            return stats;
        }

        // useful for min/max and percentiles
        Collections.sort(responseTimes);

        stats.put("min", responseTimes.get(0));
        stats.put("max", responseTimes.get(responseTimes.size() - 1));

        // Convert List<Integer> to double[] for Apache Commons Math
        double[] responseTimesArray = responseTimes.stream()
                .mapToDouble(Integer::doubleValue)
                .toArray();

        // The default constructor uses an estimation type R-7 (Excel's PERCENTILE.INC method)
        Percentile percentileCalculator = new Percentile();
        percentileCalculator.setData(responseTimesArray);

        // The evaluate method returns a double, so we cast to int as per original structure
        stats.put("50_percentile", (int) percentileCalculator.evaluate(50.0));
        stats.put("90_percentile", (int) percentileCalculator.evaluate(90.0));
        stats.put("95_percentile", (int) percentileCalculator.evaluate(95.0));
        stats.put("99_percentile", (int) percentileCalculator.evaluate(99.0));

        return stats;
    }

    // Calculate Status Code category counts
    public Map<String, Integer> getStatusCodeCategoryCounts() {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("2XX", 0);
        counts.put("4XX", 0);
        counts.put("5XX", 0);

        for (int code : statusCodes) {
            if (code >= 200 && code < 300) {
                counts.put("2XX", counts.get("2XX") + 1);
            } else if (code >= 400 && code < 500) {
                counts.put("4XX", counts.get("4XX") + 1);
            } else if (code >= 500 && code < 600) {
                counts.put("5XX", counts.get("5XX") + 1);
            }
        }
        return counts;
    }


}