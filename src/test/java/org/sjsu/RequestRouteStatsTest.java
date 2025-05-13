package org.sjsu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RequestRouteStatsTest {

    private RequestRouteStats stats;

    @BeforeEach
    void setUp() {
        stats = new RequestRouteStats();
    }

    // --- Response Time Tests ---

    @Test
    void testGetResponseTimeStats_Empty() {
        Map<String, Object> timeStats = stats.getResponseTimeStats();

        assertEquals(0, timeStats.get("min"));
        assertEquals(0, timeStats.get("max"));
        assertEquals(0, timeStats.get("50_percentile"));
        assertEquals(0, timeStats.get("90_percentile"));
        assertEquals(0, timeStats.get("95_percentile"));
        assertEquals(0, timeStats.get("99_percentile"));
    }

    @Test
    void testGetResponseTimeStats_SingleValue() {
        stats.addResponseTime(150);
        Map<String, Object> timeStats = stats.getResponseTimeStats();

        assertEquals(150, timeStats.get("min"));
        assertEquals(150, timeStats.get("max"));
        // For a single value, all percentiles should typically be that value with default percentile calc
        assertEquals(150.0, (Double) timeStats.get("50_percentile"), 0.001);
        assertEquals(150.0, (Double) timeStats.get("90_percentile"), 0.001);
        assertEquals(150.0, (Double) timeStats.get("95_percentile"), 0.001);
        assertEquals(150.0, (Double) timeStats.get("99_percentile"), 0.001);
    }

    @Test
    void testGetResponseTimeStats_MultipleValues() {
        // Test data (10 values) for easier percentile calculation checks
        stats.addResponseTime(100);
        stats.addResponseTime(200);
        stats.addResponseTime(50);
        stats.addResponseTime(150);
        stats.addResponseTime(250);
        stats.addResponseTime(120);
        stats.addResponseTime(180);
        stats.addResponseTime(90);
        stats.addResponseTime(210);
        stats.addResponseTime(160);
        // Sorted: 50, 90, 100, 120, 150, 160, 180, 200, 210, 250

        Map<String, Object> timeStats = stats.getResponseTimeStats();

        assertEquals(50, timeStats.get("min"));
        assertEquals(250, timeStats.get("max"));

        // Expected values using Apache Commons Math Percentile default (R-7)

        assertEquals(155.0, (Double) timeStats.get("50_percentile"), 0.001);
        assertEquals(214.0, (Double) timeStats.get("90_percentile"), 0.001);
        assertEquals(232.0, (Double) timeStats.get("95_percentile"), 0.001);
        assertEquals(246.4, (Double) timeStats.get("99_percentile"), 0.001);
    }


    // --- Status Code Tests ---

    @Test
    void testGetStatusCodeCategoryCounts_Empty() {
        Map<String, Integer> codeCounts = stats.getStatusCodeCategoryCounts();
        assertEquals(0, codeCounts.get("2XX"));
        assertEquals(0, codeCounts.get("4XX"));
        assertEquals(0, codeCounts.get("5XX"));
        assertEquals(3, codeCounts.size()); // Ensure no other keys
    }

    @Test
    void testGetStatusCodeCategoryCounts_MixedCodes() {
        stats.addStatusCode(200);
        stats.addStatusCode(201);
        stats.addStatusCode(404);
        stats.addStatusCode(500);
        stats.addStatusCode(503);
        stats.addStatusCode(299); // Edge case 2XX
        stats.addStatusCode(400);
        stats.addStatusCode(499); // Edge case 4XX
        stats.addStatusCode(599); // Edge case 5XX
        stats.addStatusCode(302); // Should be ignored
        stats.addStatusCode(101); // Should be ignored

        Map<String, Integer> codeCounts = stats.getStatusCodeCategoryCounts();
        assertEquals(3, codeCounts.get("2XX"));
        assertEquals(3, codeCounts.get("4XX"));
        assertEquals(3, codeCounts.get("5XX"));
        assertEquals(3, codeCounts.size());
    }

    @Test
    void testGetStatusCodeCategoryCounts_OnlyOneCategory() {
        stats.addStatusCode(400);
        stats.addStatusCode(401);
        stats.addStatusCode(403);

        Map<String, Integer> codeCounts = stats.getStatusCodeCategoryCounts();
        assertEquals(0, codeCounts.get("2XX"));
        assertEquals(3, codeCounts.get("4XX"));
        assertEquals(0, codeCounts.get("5XX"));
    }
}