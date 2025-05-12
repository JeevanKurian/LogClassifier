package org.sjsu;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles log lines related to Application events (INFO, ERROR, etc.).
 * Parses lines containing "level=".
 */
public class ApplicationLogHandler implements LogHandler {

    private LogHandler nextHandler;

    // Pattern to find "level=LEVEL_NAME"
    private static final Pattern APP_LOG_PATTERN = Pattern.compile(".*?\\blevel=([^\\s]+)\\b.*");

    @Override
    public void setNext(LogHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public boolean handle(String logLine, LogDataAggregator aggregator) {
        Matcher matcher = APP_LOG_PATTERN.matcher(logLine);

        if (matcher.matches()) {
            String level = matcher.group(1).toUpperCase(); // Extract and normalize level

            // Check if it's one of the expected levels
            if ("INFO".equals(level) || "ERROR".equals(level) || "WARNING".equals(level) || "DEBUG".equals(level)) {
                aggregator.incrementLogLevelCount(level);
                return true; // Line handled
            }
            // If level= found but value is unknown, we could log it or just pass it on.
            // Let's pass it on by falling through.
        }

        // If not handled or level was unrecognized, pass to the next in the chain
        if (nextHandler != null) {
            return nextHandler.handle(logLine, aggregator);
        }

        return false; // End of chain or no next handler
    }
}