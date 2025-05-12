package org.sjsu;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApmLogHandler implements LogHandler {

    private LogHandler nextHandler;
    private final ApmAggregator apmAggregator; // Store its specific aggregator

    private static final Pattern APM_PATTERN = Pattern.compile(
            ".*?\\bmetric=([^\\s]+).*?\\bvalue=(\\d+(\\.\\d+)?)\\b.*"
    );

    // Constructor to inject the ApmAggregator
    public ApmLogHandler(ApmAggregator apmAggregator) {
        this.apmAggregator = apmAggregator;
    }

    @Override
    public void setNext(LogHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public boolean handle(String logLine) { // Signature changed
        Matcher matcher = APM_PATTERN.matcher(logLine);

        if (matcher.matches()) {
            String metricName = matcher.group(1);
            String valueString = matcher.group(2);
            try {
                double value = Double.parseDouble(valueString);
                this.apmAggregator.addApmMetric(metricName, value);
                return true; // Line handled by this handler
            } catch (NumberFormatException e) {
                System.err.println("APM Handler: Could not parse value '" + valueString + "' in line: " + logLine);
                // Return false as it wasn't successfully handled for aggregation
                return false;
            }
        } else if (nextHandler != null) {
            return nextHandler.handle(logLine);
        }
        return false; // Not handled by this handler or any subsequent one in its path
    }
}