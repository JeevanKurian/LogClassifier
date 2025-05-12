package org.sjsu;

/**
 * Interface for log handling components in the Chain of Responsibility.
 */
public interface LogHandler {

    /**
     * Sets the next handler in the chain.
     * @param nextHandler The next LogHandler instance.
     */
    void setNext(LogHandler nextHandler);

    /**
     * Attempts to handle the provided log line. If the handler can process
     * this line type, it extracts the data and updates the aggregator.
     * If it cannot process the line, it passes the line to the next handler
     * in the chain (if one exists).
     *
     * @param logLine The raw log line string.
     * @param aggregator The LogDataAggregator instance to store extracted data.
     * @return true if the log line was handled (by this handler or a subsequent one), false otherwise.
     */
    boolean handle(String logLine, LogDataAggregator aggregator);
}