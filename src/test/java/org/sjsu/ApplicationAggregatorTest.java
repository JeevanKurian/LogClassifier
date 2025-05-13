package org.sjsu;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ApplicationAggregatorTest {

    private ApplicationAggregator applicationAggregator;
    private Gson gson;

    @BeforeEach
    void setUp() {
        applicationAggregator = new ApplicationAggregator();
        gson = new Gson();
    }

    @Test
    void testGetApplicationJson_Empty() {
        String jsonOutput = applicationAggregator.getApplicationJson();
        Type type = new TypeToken<Map<String, Integer>>() {}.getType();
        Map<String, Integer> resultMap = gson.fromJson(jsonOutput, type);

        assertTrue(resultMap.isEmpty(), "JSON output should be an empty map for no logs.");
    }

    @Test
    void testIncrementLogLevelCountAndGetJson_SingleLevel() {
        applicationAggregator.incrementLogLevelCount("INFO");
        applicationAggregator.incrementLogLevelCount("INFO");

        String jsonOutput = applicationAggregator.getApplicationJson();
        Type type = new TypeToken<Map<String, Integer>>() {}.getType();
        Map<String, Integer> resultMap = gson.fromJson(jsonOutput, type);

        assertEquals(1, resultMap.size());
        assertEquals(2, resultMap.get("INFO").intValue());
    }

    @Test
    void testIncrementLogLevelCountAndGetJson_MultipleLevels() {
        applicationAggregator.incrementLogLevelCount("INFO");
        applicationAggregator.incrementLogLevelCount("ERROR");
        applicationAggregator.incrementLogLevelCount("INFO");
        applicationAggregator.incrementLogLevelCount("WARNING");
        applicationAggregator.incrementLogLevelCount("ERROR");
        applicationAggregator.incrementLogLevelCount("ERROR");
        applicationAggregator.incrementLogLevelCount("DEBUG");
        applicationAggregator.incrementLogLevelCount("TRACE");

        String jsonOutput = applicationAggregator.getApplicationJson();
        Type type = new TypeToken<Map<String, Integer>>() {}.getType();
        Map<String, Integer> resultMap = gson.fromJson(jsonOutput, type);

        assertEquals(5, resultMap.size()); // INFO, ERROR, WARNING, DEBUG, TRACE
        assertEquals(2, resultMap.get("INFO").intValue());
        assertEquals(3, resultMap.get("ERROR").intValue());
        assertEquals(1, resultMap.get("WARNING").intValue());
        assertEquals(1, resultMap.get("DEBUG").intValue());
        assertEquals(1, resultMap.get("TRACE").intValue());
        assertNull(resultMap.get("OTHER_LEVEL"), "Should not contain levels not added.");
    }

    @Test
    void testAggregatorReceivesNormalizedUpperCaseLogLevels() {
        // We only pass uppercase keys, simulating the handler's behavior
        applicationAggregator.incrementLogLevelCount("ERROR");
        applicationAggregator.incrementLogLevelCount("ERROR");

        String jsonOutput = applicationAggregator.getApplicationJson();
        Type type = new TypeToken<Map<String, Integer>>() {}.getType();
        Map<String, Integer> resultMap = gson.fromJson(jsonOutput, type);

        assertEquals(1, resultMap.size(), "Should only have one entry for 'ERROR' after normalization by handler.");
        assertNotNull(resultMap.get("ERROR"), "'ERROR' key should exist.");
        assertEquals(2, resultMap.get("ERROR").intValue(), "Count for 'ERROR' should be 2.");
        assertNull(resultMap.get("error"), "Lowercase 'error' key should not exist if handler normalizes.");
    }
}