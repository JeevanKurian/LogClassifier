package org.sjsu;

/**
 * Interface for log handling components in the Chain of Responsibility.
 */
public interface LogHandler {

    void setNext(LogHandler nextHandler);


    boolean handle(String logLine);
}