package org.sjsu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.*;

public class ApmAggregator {
    private final Map<String, List<Double>> apmMetrics;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create(); // Pretty printing version

    public ApmAggregator(){
        this.apmMetrics = new HashMap<>();

    }
    // Methods for Handlers to Add Data
    public void addApmMetric(String metricName, double value) {
        this.apmMetrics.computeIfAbsent(metricName, k -> new ArrayList<>()).add(value);
    }


    // --- Methods for Final Aggregation & JSON Generation ---

    public String getApmJson() {
        Map<String, Map<String, Object>> apmResults = new HashMap<>();
        for (Map.Entry<String, List<Double>> entry : apmMetrics.entrySet()) {
            String metricName = entry.getKey();
            List<Double> values = entry.getValue();
            if (values == null || values.isEmpty()) {
                continue;
            }

            Collections.sort(values);

            Map<String, Object> stats = new HashMap<>();
            stats.put("minimum", values.get(0));
            stats.put("max", values.get(values.size() - 1));

            double sum = 0;
            for (double v : values) {
                sum += v;
            }
            // Use Double division for average
            stats.put("average", values.isEmpty() ? 0.0 : sum / values.size());

            double median;
            int size = values.size();
            if (size % 2 == 0) {
                median = (values.get(size / 2 - 1) + values.get(size / 2)) / 2.0;
            } else {
                median = values.get(size / 2);
            }
            stats.put("median", median);

            apmResults.put(metricName, stats);
        }
        return gson.toJson(apmResults);
    }
}
