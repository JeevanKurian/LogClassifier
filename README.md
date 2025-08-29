# LogClassifier

## Description

This project is a command-line tool designed to process a stream of diverse log entries from a text file. The application parses and categorizes each log line into one of three types: Application Performance Monitoring (APM), Application, or Request logs. After categorization, it aggregates the data and generates structured JSON files as output for each category, gracefully ignoring any corrupted or incompatible log lines.

## Features

* **Parse and Categorize:** Interprets each log line to identify its type and extract relevant data.
* **Aggregate Data:** Performs specific calculations and data summarizations based on the log's category.
    * **APM Logs:** Calculates minimum, maximum, median, and average for performance metrics.
    * **Application Logs:** Counts the occurrences of different log levels (e.g., INFO, ERROR, WARNING).
    * **Request Logs:** Calculates response time percentiles (50th, 90th, 95th, 99th) and provides counts for status code categories (2XX, 4XX, 5XX).
* **JSON Output:** Generates three distinct JSON files (`apm.json`, `application.json`, `request.json`) containing the aggregated data for each log category.
* **Extensible Design:** Built to be flexible, allowing for the easy addition of new log types in the future without major overhauls.

---

## Design Pattern: Chain of Responsibility ⛓️

The primary design pattern used in this project is the **Chain of Responsibility** pattern.

The core idea is to create a chain of processing objects, called **handlers**. When a request (in this case, a single log line) comes in, it's passed along the chain from one handler to the next until one of them successfully processes it.

In this project, the chain is set up as follows:

`ApmLogHandler` → `ApplicationLogHandler` → `RequestLogHandler`

When a new log line is read, it is first given to the `ApmLogHandler`. If the handler recognizes and processes the line, the process stops. If not, it passes the log line to the next handler in the chain, and so on.

### Why Was This Pattern Chosen?

The Chain of Responsibility pattern was an ideal choice for this project for several key reasons:

* **Handles Diverse Log Types**: The input file contains a mix of different log formats (APM, Application, and Request). The chain allows the program to identify which type of log it's dealing with without a complex block of `if-else` statements.
* **Decouples the Sender and Receiver**: The main part of the program that reads the file (the "sender") doesn't need to know how the logs are processed. It just hands off the log line to the start of the chain.
* **Excellent for Extensibility**: This is a major benefit. To support a new log type (e.g., "Security Logs"), you would just need to create a new handler and add it to the chain without modifying existing code. This adheres to the **Open/Closed Principle**.
* **Promotes the Single Responsibility Principle**: Each handler has one job: to process its specific type of log. This makes the code for each handler cleaner, easier to understand, and simpler to test.

### Potential Disadvantages

While the pattern is a good fit, there are a few potential drawbacks:

* **No Guarantee of Handling**: It's possible for a log line to go through the entire chain without being processed (e.g., if it's a corrupted or unknown format). The program addresses this by printing a warning for any unhandled lines.
* **Order Dependency**: The order of handlers in the chain can be crucial. If one handler's matching criteria is a subset of another's, the more specific handler should be placed earlier in the chain.
* **Performance Overhead**: A very long chain could introduce a minor performance cost as a log line passes through multiple handlers. For a small number of handlers like in this project, this is not a significant issue.

---

## How to Run

1.  **Prerequisites:**
    * Java JDK must be installed.

2.  **Build the Project:**
    Open a terminal in the root directory of the project and run the Gradle wrapper to build the application.
    * For macOS/Linux:
        ```sh
        ./gradlew build
        ```
    * For Windows:
        ```cmd
        ./gradlew.bat build
        ```

3.  **Execute the Program:**
    Use the `run` task in the Gradle wrapper to execute the application. The program requires the `--file` argument followed by the path to the input log file. For example, to process the `input.txt` file:
    * For macOS/Linux:
        ```sh
        ./gradlew run --args="--file input.txt"
        ```
    * For Windows:
        ```cmd
        ./gradlew.bat run --args="--file input.txt"
        ```

4.  **Check the Output:**
    After execution, three files will be generated in the root directory: `apm.json`, `application.json`, and `request.json`.

---

## Dependencies

* **Google Gson**
* **Apache Commons Math3**
* **JUnit 5**
* **Mockito**
