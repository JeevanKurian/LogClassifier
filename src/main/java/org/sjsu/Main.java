package org.sjsu;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        String inputFileName = null;

        for (int i = 0; i < args.length; i++) {
            if ("--file".equals(args[i]) && i + 1 < args.length) {
                inputFileName = args[i + 1];
                break;
            }
        }

        if (inputFileName == null) {
            System.err.println("Error: Input file not specified. Use --file <filename.txt>");
            System.exit(1);
        }

        System.out.println("Processing log file: " + inputFileName);

        // Initialize individual Aggregators
        ApmAggregator apmAggregator = new ApmAggregator();
        ApplicationAggregator applicationAggregator = new ApplicationAggregator();
        RequestAggregator requestAggregator = new RequestAggregator();

        // Create handler instances, injecting their respective aggregators
        LogHandler apmHandler = new ApmLogHandler(apmAggregator);
        LogHandler appHandler = new ApplicationLogHandler(applicationAggregator);
        LogHandler reqHandler = new RequestLogHandler(requestAggregator);

        // Build the Chain of Responsibility
        apmHandler.setNext(appHandler);
        appHandler.setNext(reqHandler);
        reqHandler.setNext(null);

        LogHandler chainStart = apmHandler;

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFileName))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                boolean handled = chainStart.handle(line);

                if (!handled) {
                    System.out.println("Warning: Line " + lineNum + " was not matched by any handler for aggregation: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file '" + inputFileName + "': " + e.getMessage());
            System.exit(1);
        }

        System.out.println("Finished processing file.");

        // Generate JSON Output from individual Aggregators (remains the same)
        String apmJson = apmAggregator.getApmJson();
        String appJson = applicationAggregator.getApplicationJson();
        String reqJson = requestAggregator.getRequestJson();

        writeJsonToFile("apm.json", apmJson);
        writeJsonToFile("application.json", appJson);
        writeJsonToFile("request.json", reqJson);

        System.out.println("Output files (apm.json, application.json, request.json) generated.");
    }

    private static void writeJsonToFile(String fileName, String jsonContent) {
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(jsonContent != null ? jsonContent : "{}");
        } catch (IOException e) {
            System.err.println("Error writing to file '" + fileName + "': " + e.getMessage());
        }
    }
}