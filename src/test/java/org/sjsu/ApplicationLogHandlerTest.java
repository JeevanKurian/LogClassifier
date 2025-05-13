// File: src/test/java/org/sjsu/ApplicationLogHandlerTest.java
package org.sjsu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApplicationLogHandlerTest {

    @Mock
    private ApplicationAggregator mockApplicationAggregator;

    @Mock
    private LogHandler mockNextHandler;

    @InjectMocks
    private ApplicationLogHandler applicationLogHandler;

    @BeforeEach
    void setUp() {
        applicationLogHandler.setNext(mockNextHandler);
    }

    /* @ParameterizedTest: Used with @ValueSource to easily test multiple valid log levels
    (both upper and lower case inputs, as the handler converts them) without writing repetitive test methods.*/

    // Test uppercase and lowercase input
    @ParameterizedTest
    @ValueSource(strings = {"INFO", "ERROR", "WARNING", "DEBUG", "TRACE", "info", "warning", "error", "debug", "trace"})
    void testHandle_ValidKnownLevel_ShouldCallAggregatorAndReturnTrue(String levelInput) {
        String logLine = "timestamp=2024-05-12T16:00:00Z level=" + levelInput + " message=\"Test message\" host=server1";
        String expectedLevel = levelInput.toUpperCase(); // Handler converts to uppercase

        boolean result = applicationLogHandler.handle(logLine);

        assertTrue(result, "Handler should return true for known level: " + levelInput);
        // Verify aggregator was called once with the UPPERCASE level
        verify(mockApplicationAggregator).incrementLogLevelCount(expectedLevel);
        // Verify no delegation occurred
        verifyNoInteractions(mockNextHandler);
    }

    @Test
    void testHandle_ValidPatternUnknownLevel_ShouldDelegate() {
        String logLine = "timestamp=2024-05-12T16:01:00Z level=AUDIT message=\"User action logged\" host=server1";
        // Assume next handler doesn't handle it
        when(mockNextHandler.handle(logLine)).thenReturn(false);

        boolean result = applicationLogHandler.handle(logLine);

        assertFalse(result, "Handler should return result from next handler for unknown level.");
        // Verify aggregator was NOT called
        verifyNoInteractions(mockApplicationAggregator);
        // Verify delegation occurred
        verify(mockNextHandler).handle(logLine);
    }

    @Test
    void testHandle_PatternNotMatched_ShouldDelegate() {
        String logLine = "timestamp=2024-05-12T16:02:00Z metric=cpu value=50 host=server1"; // No 'level='
        // Assume next handler handles it
        when(mockNextHandler.handle(logLine)).thenReturn(true);

        boolean result = applicationLogHandler.handle(logLine);

        assertTrue(result, "Handler should return result from next handler when pattern doesn't match.");
        // Verify aggregator was NOT called
        verifyNoInteractions(mockApplicationAggregator);
        // Verify delegation occurred
        verify(mockNextHandler).handle(logLine);
    }

    @Test
    void testHandle_PatternNotMatched_NoNextHandler_ShouldReturnFalse() {
        applicationLogHandler.setNext(null); // No next handler set
        String logLine = "timestamp=2024-05-12T16:02:00Z metric=cpu value=50 host=server1"; // No 'level='

        boolean result = applicationLogHandler.handle(logLine);

        assertFalse(result, "Handler should return false when pattern doesn't match and no next handler exists.");
        // Verify aggregator was NOT called
        verifyNoInteractions(mockApplicationAggregator);
    }

    @Test
    void testHandle_ValidPatternUnknownLevel_NoNextHandler_ShouldReturnFalse() {
        applicationLogHandler.setNext(null); // No next handler set
        String logLine = "timestamp=2024-05-12T16:01:00Z level=AUDIT message=\"User action logged\" host=server1";

        boolean result = applicationLogHandler.handle(logLine);

        assertFalse(result, "Handler should return false for unknown level when no next handler exists.");
        // Verify aggregator was NOT called
        verifyNoInteractions(mockApplicationAggregator);
    }

    @Test
    void testHandle_EmptyLine_ShouldDelegate() {
        String logLine = "";
        when(mockNextHandler.handle(logLine)).thenReturn(false);

        boolean result = applicationLogHandler.handle(logLine);

        assertFalse(result, "Handler should delegate empty line.");
        // Verify aggregator was NOT called
        verifyNoInteractions(mockApplicationAggregator);
        // Verify delegation occurred
        verify(mockNextHandler).handle(logLine);
    }
}