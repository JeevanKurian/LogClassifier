package org.sjsu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder; //For pretty printing

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores and aggregates data parsed from log lines.
 * Also,responsible for final calculations and generating JSON output using Gson.
 */
public class LogDataAggregator {

    // Data Storage
    private final Map<String, List<Double>> apmMetrics;
    private final Map<String, Integer> appLogLevelCounts;
    private final Map<String, RequestRouteStats> requestStats;

    // Gson instance (can be reused)
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create(); // Pretty printing version


    // Constructor
    public LogDataAggregator() {
        this.apmMetrics = new HashMap<>();
        this.appLogLevelCounts = new HashMap<>();
        this.requestStats = new HashMap<>();
    }

    // Methods for Handlers to Add Data
    public void addApmMetric(String metricName, double value) {
        this.apmMetrics.computeIfAbsent(metricName, k -> new ArrayList<>()).add(value);
    }

    public void incrementLogLevelCount(String level) {
        this.appLogLevelCounts.put(level, this.appLogLevelCounts.getOrDefault(level, 0) + 1);
    }

    public void addRequestData(String route, int statusCode, int responseTimeMs) {
        RequestRouteStats stats = this.requestStats.computeIfAbsent(route, k -> new RequestRouteStats());
        stats.addResponseTime(responseTimeMs);
        stats.addStatusCode(statusCode);
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

    public String getApplicationJson() {
        return gson.toJson(this.appLogLevelCounts);
    }

    public String getRequestJson() {
        Map<String, Map<String, Object>> requestResults = new HashMap<>();
        for(Map.Entry<String, RequestRouteStats> entry : requestStats.entrySet()){
            String route = entry.getKey();
            RequestRouteStats stats = entry.getValue();

            Map<String, Object> routeData = new HashMap<>();
            routeData.put("response_times", stats.getResponseTimeStats());
            routeData.put("status_codes", stats.getStatusCodeCategoryCounts());

            requestResults.put(route, routeData);
        }
        return gson.toJson(requestResults);
    }


}