// File: src/test/java/org/sjsu/RequestLogHandlerTest.java
package org.sjsu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RequestLogHandlerTest {

    @Mock
    private RequestAggregator mockRequestAggregator;

    @Mock
    private LogHandler mockNextHandler;

    @InjectMocks
    private RequestLogHandler requestLogHandler;

    @BeforeEach
    void setUp() {
        requestLogHandler.setNext(mockNextHandler);
    }

    @Test
    void testHandle_ValidRequestLog_ShouldCallAggregatorAndReturnTrue() {
        String logLine = "timestamp=2024-02-24T16:22:25Z request_method=POST request_url=\"/api/update\" response_status=202 response_time_ms=200 host=webserver1";
        boolean result = requestLogHandler.handle(logLine);

        assertTrue(result, "Handler should return true for a valid request log.");
        // Verify that addRequestData was called with the correct arguments
        verify(mockRequestAggregator).addRequestData("/api/update", 202, 200);
        verifyNoInteractions(mockNextHandler); // No delegation should occur
    }

    @Test
    void testHandle_ValidRequestLog_DifferentValues_ShouldCallAggregatorAndReturnTrue() {
        String logLine = "timestamp=2024-02-24T16:22:40Z request_method=GET request_url=\"/api/status\" response_status=200 response_time_ms=100 host=webserver1";
        boolean result = requestLogHandler.handle(logLine);

        assertTrue(result, "Handler should return true for a valid request log.");
        verify(mockRequestAggregator).addRequestData("/api/status", 200, 100);
        verifyNoInteractions(mockNextHandler);
    }


    @Test
    void testHandle_NonRequestLog_ShouldDelegateToNextHandler() {
        String logLine = "timestamp=2024-02-24T16:22:15Z metric=cpu_usage_percent host=webserver1 value=72"; // APM Log
        when(mockNextHandler.handle(logLine)).thenReturn(true); // Assume next handler processes it

        boolean result = requestLogHandler.handle(logLine);

        assertTrue(result, "Handler should return the result from the next handler.");
        verify(mockNextHandler).handle(logLine); // Verify delegation
        verifyNoInteractions(mockRequestAggregator); // Aggregator should not be called
    }

    @Test
    void testHandle_NonRequestLog_NoNextHandler_ShouldReturnFalse() {
        requestLogHandler.setNext(null); // No next handler
        String logLine = "timestamp=2024-02-24T16:22:20Z level=INFO message=\"Scheduled maintenance\" host=webserver1"; // App Log

        boolean result = requestLogHandler.handle(logLine);

        assertFalse(result, "Handler should return false if it doesn't match and no next handler exists.");
        verifyNoInteractions(mockRequestAggregator);
    }

    @Test
    void testHandle_MalformedRequestLog_StatusNotNumeric_ShouldNotMatchAndDelegate() {
        // The regex for status is \d+, so "abc" won't match the pattern.
        String logLine = "timestamp=... request_url=\"/api/data\" response_status=abc response_time_ms=100 ...";
        when(mockNextHandler.handle(logLine)).thenReturn(false);

        boolean result = requestLogHandler.handle(logLine);

        assertFalse(result, "Handler should delegate if status is not numeric (pattern mismatch).");
        verify(mockNextHandler).handle(logLine); // Verify delegation occurred
        verifyNoInteractions(mockRequestAggregator); // Aggregator should not be called
    }

    @Test
    void testHandle_MalformedRequestLog_TimeNotNumeric_ShouldNotMatchAndDelegate() {
        // The regex for time is \d+, so "xyz" won't match the pattern.
        String logLine = "timestamp=... request_url=\"/api/data\" response_status=200 response_time_ms=xyz ...";
        when(mockNextHandler.handle(logLine)).thenReturn(false);

        boolean result = requestLogHandler.handle(logLine);

        assertFalse(result, "Handler should delegate if time is not numeric (pattern mismatch).");
        verify(mockNextHandler).handle(logLine); // Verify delegation occurred
        verifyNoInteractions(mockRequestAggregator); // Aggregator should not be called
    }

    @Test
    void testHandle_RequestLogMissingUrl_ShouldNotMatchAndDelegate() {
        String logLine = "timestamp=... response_status=200 response_time_ms=100 ..."; // Missing request_url
        when(mockNextHandler.handle(logLine)).thenReturn(false);

        boolean result = requestLogHandler.handle(logLine);

        assertFalse(result, "Handler should delegate if request_url is missing.");
        verify(mockNextHandler).handle(logLine);
        verifyNoInteractions(mockRequestAggregator);
    }

    @Test
    void testHandle_RequestLogMissingStatus_ShouldNotMatchAndDelegate() {
        String logLine = "timestamp=... request_url=\"/api/data\" response_time_ms=100 ..."; // Missing response_status
        when(mockNextHandler.handle(logLine)).thenReturn(false);

        boolean result = requestLogHandler.handle(logLine);

        assertFalse(result, "Handler should delegate if response_status is missing.");
        verify(mockNextHandler).handle(logLine);
        verifyNoInteractions(mockRequestAggregator);
    }

    @Test
    void testHandle_RequestLogMissingTime_ShouldNotMatchAndDelegate() {
        String logLine = "timestamp=... request_url=\"/api/data\" response_status=200 ..."; // Missing response_time_ms
        when(mockNextHandler.handle(logLine)).thenReturn(false);

        boolean result = requestLogHandler.handle(logLine);

        assertFalse(result, "Handler should delegate if response_time_ms is missing.");
        verify(mockNextHandler).handle(logLine);
        verifyNoInteractions(mockRequestAggregator);
    }

    @Test
    void testHandle_EmptyLine_ShouldDelegate() {
        String logLine = "";
        when(mockNextHandler.handle(logLine)).thenReturn(false);

        boolean result = requestLogHandler.handle(logLine);

        assertFalse(result, "Handler should delegate empty lines.");
        verify(mockNextHandler).handle(logLine);
        verifyNoInteractions(mockRequestAggregator);
    }
}