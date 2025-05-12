package org.sjsu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;

public class RequestAggregator {
    private final Map<String, RequestRouteStats> requestStats;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create(); // Pretty printing version

    RequestAggregator(){
        this.requestStats = new HashMap<>();

    }
    // Methods for Handlers to Add Data
    public void addRequestData(String route, int statusCode, int responseTimeMs) {
        RequestRouteStats stats = this.requestStats.computeIfAbsent(route, k -> new RequestRouteStats());
        stats.addResponseTime(responseTimeMs);
        stats.addStatusCode(statusCode);
    }

    // --- Methods for Final Aggregation & JSON Generation ---
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
