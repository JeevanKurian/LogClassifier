Log Processing Application
==========================

Objective
---------

The **Log Processing Application** is a command-line tool that parses log files (`.txt`) containing diverse log entries. Each line represents a distinct log entry, which the application categorizes and aggregates into JSON files.

It currently supports:

-   **APM (Application Performance Metrics) Logs**

-   **Application Logs**

-   **Request Logs**

The architecture is **extensible**, enabling future support for additional log types and formats.

* * * * *

Log File Types and Aggregations
-------------------------------

### 1\. APM Logs

APM Logs track application performance metrics such as CPU, memory, and disk usage.

**Aggregation (`apm.json`):**

`{
  "cpu_usage_percent": {
    "minimum": 60,
    "median": 78,
    "average": 77,
    "max": 90
  },
  "memory_usage_percent": {
    "minimum": 5,
    "median": 10,
    "average": 13.5,
    "max": 78
  }
}`

* * * * *

### 2\. Application Logs

Application Logs provide insights into operational events, including errors, warnings, and info messages.

**Aggregation (`application.json`):**

`{
  "ERROR": 2,
  "INFO": 3,
  "DEBUG": 1,
  "WARNING": 1
}`

* * * * *

### 3\. Request Logs

Request Logs detail HTTP requests, response times, and status codes.

**Aggregation (`request.json`):**

`{
  "/api/update": {
    "response_times": {
      "min": 200,
      "50_percentile": 200,
      "90_percentile": 200,
      "95_percentile": 200,
      "99_percentile": 200,
      "max": 200
    },
    "status_codes": {
      "2XX": 1,
      "4XX": 0,
      "5XX": 0
    }
  },
  "/api/status": {
    "response_times": {
      "min": 100,
      "50_percentile": 100,
      "90_percentile": 100,
      "95_percentile": 100,
      "99_percentile": 100,
      "max": 100
    },
    "status_codes": {
      "2XX": 1,
      "4XX": 0,
      "5XX": 0
    }
  }
}`

* * * * *

Sample Log Format
-----------------

Example log lines from `input.txt`:

`timestamp=2024-11-24T10:01:30Z metric=cpu_usage_percent host=webserver1 value=94
timestamp=2024-11-24T10:02:55Z level=ERROR message="File not found" file_path="/var/app/config.yml" host=webserver2
timestamp=2024-11-24T10:01:25Z request_method=PUT request_url="/api/update" response_status=503 response_time_ms=61 host=webserver1`

* * * * *

Design Pattern: Chain of Responsibility ⛓️
------------------------------------------

The primary design pattern used in this project is **Chain of Responsibility**.

### How It Works

-   A chain of processing objects, called **handlers**, is created.

-   Each incoming log line is passed along the chain until a handler recognizes and processes it.

**Handler order in this project:**

`ApmLogHandler → ApplicationLogHandler → RequestLogHandler`

1.  A new log line is given to `ApmLogHandler`.

2.  If it processes the line, the chain stops.

3.  Otherwise, the line is passed to the next handler, and so on.

### Why This Pattern?

-   **Handles Diverse Log Types:** Avoids large `if-else` blocks by delegating responsibility to specialized handlers.

-   **Decouples Sender and Receiver:** The file reader (sender) does not need to know processing details.

-   **Extensible:** Add a new log type (e.g., Security Logs) by adding a new handler---no changes to existing handlers.

-   **Single Responsibility Principle:** Each handler focuses on one log type, simplifying maintenance and testing.

### Potential Disadvantages

-   **No Guarantee of Handling:** Unknown/corrupted lines may pass through all handlers; the program logs a warning.

-   **Order Dependency:** Overlapping criteria require careful handler ordering.

-   **Performance Overhead:** Very long chains can add minor overhead (negligible for a small number of handlers).

* * * * *

How to Run
----------

### Prerequisites

-   **Java JDK** installed.

### Build the Project

From the project root:

**macOS/Linux**

`./gradlew build`

**Windows**

`.\gradlew.bat build`

### Execute the Program

Run with the `--file` argument pointing to your input log file:

**macOS/Linux**

`./gradlew run --args="--file input.txt"`

**Windows**

`.\gradlew.bat run --args="--file input.txt"`

### Check the Output

After execution, the following files are generated in the project root:

-   `apm.json`

-   `application.json`

-   `request.json`

* * * * *

Dependencies
------------

-   Google Gson

-   Apache Commons Math3

-   JUnit 5

-   Mockito
