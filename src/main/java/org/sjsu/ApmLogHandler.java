package org.sjsu;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles log lines related to Application Performance Metrics (APM).
 * Parses lines containing "metric=" and "value=".
 */
public class ApmLogHandler implements LogHandler {

    private LogHandler nextHandler;

    // Pattern looks for "metric=some_name" and "value=some_number"
    private static final Pattern APM_PATTERN = Pattern.compile(
            ".*?\\bmetric=([^\\s]+).*?\\bvalue=(\\d+(\\.\\d+)?)\\b.*"
    );

    @Override
    public void setNext(LogHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public boolean handle(String logLine, LogDataAggregator aggregator) {
        Matcher matcher = APM_PATTERN.matcher(logLine);

        if (matcher.matches()) {
            String metricName = matcher.group(1);
            String valueString = matcher.group(2);

            try {
                double value = Double.parseDouble(valueString);
                aggregator.addApmMetric(metricName, value);
                return true; // Line handled
            } catch (NumberFormatException e) {
                System.err.println("APM Handler: Could not parse value '" + valueString + "' in line: " + logLine);
                return false; // Failed to parse value, treat as unhandled
            }
        } else if (nextHandler != null) {
            return nextHandler.handle(logLine, aggregator); // Pass to the next handler
        }
        return false; // End of chain or no next handler
    }
}