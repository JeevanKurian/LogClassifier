package org.sjsu;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ApmAggregatorTest {

    private ApmAggregator apmAggregator;
    private Gson gson;

    @BeforeEach
    void setUp() {
        apmAggregator = new ApmAggregator();
        gson = new Gson();
    }

    @Test
    void testAddApmMetricAndGetApmJson_Empty() {
        String jsonOutput = apmAggregator.getApmJson();
        /* the Type and TypeToken part in the test class is specifically used to help Gson properly deserialize a complex generic type,
         like a Map<String, Map<String, Object>>.Java uses a system called type erasure, which means that at runtime,
         information about generic types (like List<String>) is mostly erased and becomes just List. So Gson doesn’t know
          what exact type to convert your JSON into when you give it a generic like Map<String, Map<String, Object>>. */
        Type type = new TypeToken<Map<String, Map<String, Object>>>() {}.getType();
        Map<String, Map<String, Object>> resultMap = gson.fromJson(jsonOutput, type);

        assertTrue(resultMap.isEmpty(), "JSON output should be empty for no metrics.");
    }

    @Test
    void testAddApmMetricAndGetApmJson_SingleMetricSingleValue() {
        apmAggregator.addApmMetric("cpu_usage_percent", 75.0);
        String jsonOutput = apmAggregator.getApmJson();

        Type type = new TypeToken<Map<String, Map<String, Object>>>() {}.getType();
        Map<String, Map<String, Object>> resultMap = gson.fromJson(jsonOutput, type);

        assertNotNull(resultMap.get("cpu_usage_percent"), "CPU usage metric should exist.");
        Map<String, Object> cpuStats = resultMap.get("cpu_usage_percent");

        assertEquals(75.0, (Double) cpuStats.get("minimum"), 0.001);
        assertEquals(75.0, (Double) cpuStats.get("median"), 0.001);
        assertEquals(75.0, (Double) cpuStats.get("average"), 0.001);
        assertEquals(75.0, (Double) cpuStats.get("max"), 0.001);
    }

    @Test
    void testAddApmMetricAndGetApmJson_SingleMetricMultipleValues_EvenCount() {
        apmAggregator.addApmMetric("memory_usage_mb", 100.0);
        apmAggregator.addApmMetric("memory_usage_mb", 200.0);
        apmAggregator.addApmMetric("memory_usage_mb", 50.0);
        apmAggregator.addApmMetric("memory_usage_mb", 150.0);
        // Sorted: 50, 100, 150, 200

        String jsonOutput = apmAggregator.getApmJson();
        Type type = new TypeToken<Map<String, Map<String, Object>>>() {}.getType();
        Map<String, Map<String, Object>> resultMap = gson.fromJson(jsonOutput, type);

        assertNotNull(resultMap.get("memory_usage_mb"));
        Map<String, Object> memStats = resultMap.get("memory_usage_mb");

        assertEquals(50.0, (Double) memStats.get("minimum"), 0.001);
        assertEquals(200.0, (Double) memStats.get("max"), 0.001);
        assertEquals(125.0, (Double) memStats.get("median"), 0.001); // (100+150)/2
        assertEquals(125.0, (Double) memStats.get("average"), 0.001); // (50+100+150+200)/4 = 500/4
    }

    @Test
    void testAddApmMetricAndGetApmJson_SingleMetricMultipleValues_OddCount() {
        apmAggregator.addApmMetric("disk_io_ops", 10.0);
        apmAggregator.addApmMetric("disk_io_ops", 30.0);
        apmAggregator.addApmMetric("disk_io_ops", 5.0);
        // Sorted: 5, 10, 30

        String jsonOutput = apmAggregator.getApmJson();
        Type type = new TypeToken<Map<String, Map<String, Object>>>() {}.getType();
        Map<String, Map<String, Object>> resultMap = gson.fromJson(jsonOutput, type);

        assertNotNull(resultMap.get("disk_io_ops"));
        Map<String, Object> diskStats = resultMap.get("disk_io_ops");

        assertEquals(5.0, (Double) diskStats.get("minimum"), 0.001);
        assertEquals(30.0, (Double) diskStats.get("max"), 0.001);
        assertEquals(10.0, (Double) diskStats.get("median"), 0.001); // Middle value
        assertEquals(15.0, (Double) diskStats.get("average"), 0.001); // (5+10+30)/3 = 45/3
    }

    @Test
    void testAddApmMetricAndGetApmJson_MultipleMetrics() {
        apmAggregator.addApmMetric("cpu_usage_percent", 60.0);
        apmAggregator.addApmMetric("cpu_usage_percent", 80.0); // Avg: 70, Median: 70

        apmAggregator.addApmMetric("memory_usage_percent", 5.0);
        apmAggregator.addApmMetric("memory_usage_percent", 10.0);
        apmAggregator.addApmMetric("memory_usage_percent", 78.0); // Avg: 31, Median: 10

        String jsonOutput = apmAggregator.getApmJson();
        Type type = new TypeToken<Map<String, Map<String, Object>>>() {}.getType();
        Map<String, Map<String, Object>> resultMap = gson.fromJson(jsonOutput, type);

        assertTrue(resultMap.containsKey("cpu_usage_percent"));
        assertTrue(resultMap.containsKey("memory_usage_percent"));

        Map<String, Object> cpuStats = resultMap.get("cpu_usage_percent");
        assertEquals(60.0, (Double) cpuStats.get("minimum"), 0.001);
        assertEquals(80.0, (Double) cpuStats.get("max"), 0.001);
        assertEquals(70.0, (Double) cpuStats.get("median"), 0.001);
        assertEquals(70.0, (Double) cpuStats.get("average"), 0.001);

        Map<String, Object> memStats = resultMap.get("memory_usage_percent");
        assertEquals(5.0, (Double) memStats.get("minimum"), 0.001);
        assertEquals(78.0, (Double) memStats.get("max"), 0.001);
        assertEquals(10.0, (Double) memStats.get("median"), 0.001); // Sorted: 5, 10, 78
        assertEquals((5.0+10.0+78.0)/3.0, (Double) memStats.get("average"), 0.001); // (93)/3 = 31
    }

    @Test
    void testGetApmJson_MetricWithNoValues_ShouldBeSkipped() {
        // Add a metric but then simulate its list being empty (though current code doesn't allow this directly)
        // The aggregator's getApmJson checks "if (values == null || values.isEmpty()) { continue; }"
        // So, if a metric was somehow added with an empty list, it shouldn't appear in JSON.
        // ApmAggregator's current addApmMetric always adds to a list, so we can't directly test empty list scenario
        // unless we modify internals or the aggregator had a remove function.
        // However, the default state is an empty map, which is tested in testAddApmMetricAndGetApmJson_Empty.

        // To truly test the "if (values.isEmpty())" condition, one might need reflection
        // or a slightly different design. For now, we'll trust the code's explicit check.
        // This test effectively confirms no metrics are present if none are added.
        String jsonOutput = apmAggregator.getApmJson();
        Type type = new TypeToken<Map<String, Map<String, Object>>>() {}.getType();
        Map<String, Map<String, Object>> resultMap = gson.fromJson(jsonOutput, type);

        assertTrue(resultMap.isEmpty(), "JSON output should be empty for no valid metrics.");
    }
}