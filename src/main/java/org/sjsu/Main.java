package org.sjsu;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class Main {

    public static void main(String[] args) {
        String inputFileName = null;

        // Parse Command Line Arguments
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

        // Initialize Aggregator and Handlers
        LogDataAggregator aggregator = new LogDataAggregator();

        // Create handler instances
        LogHandler apmHandler = new ApmLogHandler();
        LogHandler appHandler = new ApplicationLogHandler();
        LogHandler reqHandler = new RequestLogHandler();

        // building the Chain
        apmHandler.setNext(appHandler);
        appHandler.setNext(reqHandler);

        LogHandler chainStart = apmHandler; //  starting point of the chain

        // Read and Process the Log File line by line
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFileName))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }
                // Pass each line to the start of the chain
                boolean handled = chainStart.handle(line, aggregator);
                if (!handled) {
                     System.out.println("Warning: Line " + lineNum + " was not processed: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file '" + inputFileName + "': " + e.getMessage());
            System.exit(1);
        }

        System.out.println("Finished processing file.");

        // Generate JSON Output from Aggregator
        String apmJson = aggregator.getApmJson();
        String appJson = aggregator.getApplicationJson();
        String reqJson = aggregator.getRequestJson();

        // Write JSON to Output Files
        // Ensure output files are created even if JSON is empty "{} with help of writeJsonToFile function"
        writeJsonToFile("apm.json", apmJson);
        writeJsonToFile("application.json", appJson);
        writeJsonToFile("request.json", reqJson);

        System.out.println("Output files (apm.json, application.json, request.json) generated.");
    }

    // Helper method to write JSON content to a file
    private static void writeJsonToFile(String fileName, String jsonContent) {
        // Use try-with-resources to ensure the writer is closed automatically
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(jsonContent != null ? jsonContent : "{}"); // Write empty JSON if content is null
        } catch (IOException e) {
            System.err.println("Error writing to file '" + fileName + "': " + e.getMessage());
        }
    }
}