package org.sjsu;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RequestAggregatorTest {

    private RequestAggregator requestAggregator;
    private Gson gson;

    @BeforeEach
    void setUp() {
        requestAggregator = new RequestAggregator();
        gson = new Gson();
    }

    @Test
    void testGetRequestJson_Empty() {
        String jsonOutput = requestAggregator.getRequestJson();
        Type type = new TypeToken<Map<String, Map<String, Object>>>() {}.getType();
        Map<String, Map<String, Object>> resultMap = gson.fromJson(jsonOutput, type);

        assertTrue(resultMap.isEmpty(), "JSON output should be an empty map for no request data.");
    }

    @Test
    void testAddRequestDataAndGetJson_SingleRouteSingleRequest() {
        requestAggregator.addRequestData("/api/test", 200, 150);
        String jsonOutput = requestAggregator.getRequestJson();

        Type type = new TypeToken<Map<String, Map<String, Object>>>() {}.getType();
        Map<String, Map<String, Object>> resultMap = gson.fromJson(jsonOutput, type);

        assertNotNull(resultMap.get("/api/test"), "Route '/api/test' should exist.");
        Map<String, Object> routeData = resultMap.get("/api/test");

        assertNotNull(routeData.get("response_times"), "Response times data should exist.");
        assertNotNull(routeData.get("status_codes"), "Status codes data should exist.");

        /*@SuppressWarnings("unchecked"): Used because we are casting Object values from
        the map to more specific map types (Map<String, Double>*/
        @SuppressWarnings("unchecked")
        Map<String, Double> responseTimes = (Map<String, Double>) routeData.get("response_times");
        assertEquals(150.0, responseTimes.get("min"), 0.001);
        assertEquals(150.0, responseTimes.get("max"), 0.001);
        assertEquals(150.0, responseTimes.get("50_percentile"), 0.001);


        @SuppressWarnings("unchecked")
        Map<String, Double> statusCodes = (Map<String, Double>) routeData.get("status_codes");
        assertEquals(1.0, statusCodes.get("2XX").doubleValue(), 0.001);
        assertEquals(0.0, statusCodes.get("4XX").doubleValue(), 0.001);
        assertEquals(0.0, statusCodes.get("5XX").doubleValue(), 0.001);
    }

    @Test
    void testAddRequestDataAndGetJson_SingleRouteMultipleRequests() {
        requestAggregator.addRequestData("/api/user", 201, 100);
        requestAggregator.addRequestData("/api/user", 404, 50);
        requestAggregator.addRequestData("/api/user", 200, 120);
        requestAggregator.addRequestData("/api/user", 500, 300);
        // For /api/user:
        // Times: 50, 100, 120, 300. Sorted: 50, 100, 120, 300
        // Min: 50, Max: 300
        // Median (50th for R_7): 100 + 0.5 * (120-100) = 100 + 10 = 110.0 (n=4, h=0.5*(4-1)+1 = 2.5)
        // Status: 201 (2XX), 404 (4XX), 200 (2XX), 500 (5XX) -> 2XX:2, 4XX:1, 5XX:1

        String jsonOutput = requestAggregator.getRequestJson();
        Type type = new TypeToken<Map<String, Map<String, Object>>>() {}.getType();
        Map<String, Map<String, Object>> resultMap = gson.fromJson(jsonOutput, type);

        assertNotNull(resultMap.get("/api/user"));
        Map<String, Object> routeData = resultMap.get("/api/user");

        @SuppressWarnings("unchecked")
        Map<String, Double> responseTimes = (Map<String, Double>) routeData.get("response_times");
        assertEquals(50.0, responseTimes.get("min"), 0.001);
        assertEquals(300.0, responseTimes.get("max"), 0.001);
        assertEquals(110.0, responseTimes.get("50_percentile"), 0.001); // Check median

        @SuppressWarnings("unchecked")
        Map<String, Double> statusCodes = (Map<String, Double>) routeData.get("status_codes");
        assertEquals(2.0, statusCodes.get("2XX").doubleValue(), 0.001);
        assertEquals(1.0, statusCodes.get("4XX").doubleValue(), 0.001);
        assertEquals(1.0, statusCodes.get("5XX").doubleValue(), 0.001);
    }

    @Test
    void testAddRequestDataAndGetJson_MultipleRoutes() {
        requestAggregator.addRequestData("/api/ping", 200, 10);
        requestAggregator.addRequestData("/api/health", 200, 15);
        requestAggregator.addRequestData("/api/ping", 200, 20); // Min:10, Max:20, Median:15 (for /api/ping)

        String jsonOutput = requestAggregator.getRequestJson();
        Type type = new TypeToken<Map<String, Map<String, Object>>>() {}.getType();
        Map<String, Map<String, Object>> resultMap = gson.fromJson(jsonOutput, type);

        assertTrue(resultMap.containsKey("/api/ping"));
        assertTrue(resultMap.containsKey("/api/health"));
        assertEquals(2, resultMap.size());

        // Check /api/ping
        Map<String, Object> pingData = resultMap.get("/api/ping");
        @SuppressWarnings("unchecked")
        Map<String, Double> pingResponseTimes = (Map<String, Double>) pingData.get("response_times");
        assertEquals(10.0, pingResponseTimes.get("min"), 0.001);
        assertEquals(20.0, pingResponseTimes.get("max"), 0.001);
        assertEquals(15.0, pingResponseTimes.get("50_percentile"), 0.001);

        @SuppressWarnings("unchecked")
        Map<String, Double> pingStatusCodes = (Map<String, Double>) pingData.get("status_codes");
        assertEquals(2.0, pingStatusCodes.get("2XX").doubleValue(), 0.001);

        // Check /api/health
        Map<String, Object> healthData = resultMap.get("/api/health");
        @SuppressWarnings("unchecked")
        Map<String, Double> healthResponseTimes = (Map<String, Double>) healthData.get("response_times");
        assertEquals(15.0, healthResponseTimes.get("min"), 0.001);

        @SuppressWarnings("unchecked")
        Map<String, Double> healthStatusCodes = (Map<String, Double>) healthData.get("status_codes");
        assertEquals(1.0, healthStatusCodes.get("2XX").doubleValue(), 0.001);
    }
}