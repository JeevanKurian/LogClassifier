package org.sjsu;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestLogHandler implements LogHandler {

    private LogHandler nextHandler;
    private final RequestAggregator requestAggregator; // Store its specific aggregator

    private static final Pattern REQ_LOG_PATTERN = Pattern.compile(
            ".*?\\brequest_url=\"(?<url>[^\"]+)\".*?" +
                    "\\bresponse_status=(?<status>\\d+).*?" +
                    "\\bresponse_time_ms=(?<time>\\d+)\\b.*"
    );

    // Constructor to inject the RequestAggregator
    public RequestLogHandler(RequestAggregator requestAggregator) {
        this.requestAggregator = requestAggregator;
    }

    @Override
    public void setNext(LogHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public boolean handle(String logLine) { // Signature changed
        Matcher matcher = REQ_LOG_PATTERN.matcher(logLine);

        if (matcher.matches()) {
            try {
                String url = matcher.group("url");
                int status = Integer.parseInt(matcher.group("status"));
                int time = Integer.parseInt(matcher.group("time"));

                this.requestAggregator.addRequestData(url, status, time);
                return true; // Line handled by this handler
            } catch (NumberFormatException e) {
                System.err.println("Request Handler: Could not parse status or time in line: " + logLine);
                return false; // Not successfully handled
            } catch (IllegalArgumentException e) {
                System.err.println("Request Handler: Error accessing named group in line: " + logLine);
                return false; // Not successfully handled
            }
        } else if (nextHandler != null) {
            return nextHandler.handle(logLine);
        }
        return false; // Not handled by this handler or any subsequent one
    }
}