package org.sjsu;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles log lines related to HTTP Requests.
 * Parses lines containing "request_url=", "response_status=", and "response_time_ms=".
 */
public class RequestLogHandler implements LogHandler {

    private LogHandler nextHandler;

    // Pattern to capture request details. Allows other fields to exist.
    // Uses named capture groups for clarity: (?<name>...)
    private static final Pattern REQ_LOG_PATTERN = Pattern.compile(
            ".*?\\brequest_url=\"(?<url>[^\"]+)\".*?" + // Capture URL within quotes
                    "\\bresponse_status=(?<status>\\d+).*?" +  // Capture status code (digits)
                    "\\bresponse_time_ms=(?<time>\\d+)\\b.*"   // Capture response time (digits)
    );
    // Simpler pattern if URL is not guaranteed to be quoted:
    // Pattern.compile(".*?\\brequest_url=([^\\s]+).*?\\bresponse_status=(\\d+).*?\\bresponse_time_ms=(\\d+)\\b.*");


    @Override
    public void setNext(LogHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public boolean handle(String logLine, LogDataAggregator aggregator) {
        Matcher matcher = REQ_LOG_PATTERN.matcher(logLine);

        if (matcher.matches()) {
            try {
                // Extract captured groups by name (or index if not using named groups)
                String url = matcher.group("url");
                int status = Integer.parseInt(matcher.group("status"));
                int time = Integer.parseInt(matcher.group("time"));

                aggregator.addRequestData(url, status, time);
                return true; // Line handled
            } catch (NumberFormatException e) {
                System.err.println("Request Handler: Could not parse status or time in line: " + logLine);
                return false; // Failed to parse numbers, treat as unhandled
            } catch (IllegalArgumentException e) {
                System.err.println("Request Handler: Error accessing named group (pattern mismatch?) in line: " + logLine);
                return false; // Pattern matched overall, but group name was wrong?
            }
        } else if (nextHandler != null) {
            // Pass to the next handler if this one couldn't process it
            return nextHandler.handle(logLine, aggregator);
        }

        return false; // End of chain or no next handler
    }
}