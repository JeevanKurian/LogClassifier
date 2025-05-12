package org.sjsu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;

public class ApplicationAggregator {
    private final Map<String, Integer> appLogLevelCounts;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create(); // Pretty printing version

    ApplicationAggregator(){
        this.appLogLevelCounts = new HashMap<>();

    }


    // Methods for Handlers to Add Data
    public void incrementLogLevelCount(String level) {
        this.appLogLevelCounts.put(level, this.appLogLevelCounts.getOrDefault(level, 0) + 1);
    }

    // --- Methods for Final Aggregation & JSON Generation ---

    public String getApplicationJson() {
        return gson.toJson(this.appLogLevelCounts);
    }
}
