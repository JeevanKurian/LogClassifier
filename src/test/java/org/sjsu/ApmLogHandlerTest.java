package org.sjsu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) //  Integrates Mockito with JUnit 5 lifecycle.
public class ApmLogHandlerTest {

/*@Mock: Creates mock objects for dependencies (ApmAggregator, LogHandler). These mocks don't have real logic;
They just record if their methods were called and return whatever value we tell them to return using when(...).*/

    @Mock // Creates a mock instance of ApmAggregator
    private ApmAggregator mockApmAggregator;

    @Mock // Creates a mock instance of the next LogHandler
    private LogHandler mockNextHandler;

    @InjectMocks // Creates an instance of ApmLogHandler and injects the mocks (@Mock fields) into it
    private ApmLogHandler apmLogHandler;

    @BeforeEach
    void setUp() {
        // Note: @InjectMocks doesn't automatically call setters like setNext
        apmLogHandler.setNext(mockNextHandler);
    }

    @Test
    void testHandle_ValidApmLog_ShouldCallAggregatorAndReturnTrue() {
        String logLine = "timestamp=2024-02-24T16:22:15Z metric=cpu_usage_percent host=webserver1 value=72.5";
        boolean result = apmLogHandler.handle(logLine);

        assertTrue(result, "Handler should return true for a valid APM log.");
        // Verify that addApmMetric was called exactly once with the correct arguments
        verify(mockApmAggregator).addApmMetric("cpu_usage_percent", 72.5);
        // Verify that the next handler was never called
        verifyNoInteractions(mockNextHandler);
    }

    @Test
    void testHandle_ValidApmLog_IntegerVal_ShouldCallAggregatorAndReturnTrue() {
        String logLine = "timestamp=2024-02-24T16:23:00Z metric=network_bytes_in host=webserver1 interface=eth0 value=543210";
        boolean result = apmLogHandler.handle(logLine);

        assertTrue(result, "Handler should return true for a valid APM log with integer value.");
        verify(mockApmAggregator).addApmMetric("network_bytes_in", 543210.0);
        verifyNoInteractions(mockNextHandler);
    }


    @Test
    void testHandle_NonApmLog_ShouldDelegateToNextHandler() {
        //log line of application type and not of apm type.
        String logLine = "timestamp=2024-02-24T16:22:20Z level=INFO message=\"Scheduled maintenance starting\" host=webserver1";
        // Define what the mock next handler should return when called with this line
        when(mockNextHandler.handle(logLine)).thenReturn(true); // Assume next handler processes it

        boolean result = apmLogHandler.handle(logLine);
        assertTrue(result, "Handler should return the result from the next handler.");

        // Verify that the next handler's handle method was called exactly once
        verify(mockNextHandler).handle(logLine);
        // Verify that our aggregator was never called
        verifyNoInteractions(mockApmAggregator);
    }

    @Test
    void testHandle_NonApmLog_NoNextHandler_ShouldReturnFalse() {
        apmLogHandler.setNext(null); // Explicitly set no next handler
        String logLine = "timestamp=2024-02-24T16:22:20Z level=INFO message=\"Scheduled maintenance starting\" host=webserver1";

        boolean result = apmLogHandler.handle(logLine);

        assertFalse(result, "Handler should return false if it doesn't match and no next handler exists.");
        verifyNoInteractions(mockApmAggregator); // Aggregator should not be called
        // No need to verify mockNextHandler as it's null
    }


    @Test
    void testHandle_ApmLogWithValueNotMatchingPattern_ShouldDelegate() {
        String logLine = "timestamp=2024-02-24T16:22:15Z metric=cpu_usage_percent host=webserver1 value=high";

        //The next handler also doesn't process it
        when(mockNextHandler.handle(logLine)).thenReturn(false);

        boolean result = apmLogHandler.handle(logLine);
        assertFalse(result, "Handler should return result from next handler when pattern doesn't match value.");

        // Verify our aggregator was not called
        verifyNoInteractions(mockApmAggregator);
        // Verify the next handler WAS called exactly once with the correct log line
        verify(mockNextHandler).handle(logLine);
    }

    @Test
    void testHandle_LineMissingValue_ShouldNotMatchAndDelegate() {
        String logLine = "timestamp=2024-02-24T16:22:15Z metric=cpu_usage_percent host=webserver1";
        when(mockNextHandler.handle(logLine)).thenReturn(false); // Assume next handler doesn't handle it either

        boolean result = apmLogHandler.handle(logLine);

        assertFalse(result, "Handler should delegate if required 'value=' part is missing.");
        verify(mockNextHandler).handle(logLine); // Should attempt to delegate
        verifyNoInteractions(mockApmAggregator); // Should not call aggregator
    }

    @Test
    void testHandle_LineMissingMetric_ShouldNotMatchAndDelegate() {
        String logLine = "timestamp=2024-02-24T16:22:15Z host=webserver1 value=75";
        when(mockNextHandler.handle(logLine)).thenReturn(false);

        boolean result = apmLogHandler.handle(logLine);

        assertFalse(result, "Handler should delegate if required 'metric=' part is missing.");
        verify(mockNextHandler).handle(logLine); // Should attempt to delegate
        verifyNoInteractions(mockApmAggregator); // Should not call aggregator
    }

    @Test
    void testHandle_EmptyLine_ShouldNotMatchAndDelegate() {
        String logLine = "";
        when(mockNextHandler.handle(logLine)).thenReturn(false);

        boolean result = apmLogHandler.handle(logLine);

        assertFalse(result, "Handler should delegate empty lines.");
        verify(mockNextHandler).handle(logLine); // Should attempt to delegate
        verifyNoInteractions(mockApmAggregator); // Should not call aggregator
    }
}