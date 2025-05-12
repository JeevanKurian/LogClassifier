package org.sjsu;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApplicationLogHandler implements LogHandler {

    private LogHandler nextHandler;
    private final ApplicationAggregator applicationAggregator; // Store its specific aggregator

    private static final Pattern APP_LOG_PATTERN = Pattern.compile(".*?\\blevel=([^\\s]+)\\b.*");

    // Constructor to inject the ApplicationAggregator
    public ApplicationLogHandler(ApplicationAggregator applicationAggregator) {
        this.applicationAggregator = applicationAggregator;
    }

    @Override
    public void setNext(LogHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public boolean handle(String logLine) { // Signature changed
        Matcher matcher = APP_LOG_PATTERN.matcher(logLine);

        if (matcher.matches()) {
            String level = matcher.group(1).toUpperCase();
            if ("INFO".equals(level) || "ERROR".equals(level) || "WARNING".equals(level) || "DEBUG".equals(level) || "TRACE".equals(level)) {
                this.applicationAggregator.incrementLogLevelCount(level);
                return true; // Line handled by this handler
            }

        }

        if (nextHandler != null) {
            return nextHandler.handle(logLine);
        }
        return false; // Not handled by this handler or any subsequent one
    }
}