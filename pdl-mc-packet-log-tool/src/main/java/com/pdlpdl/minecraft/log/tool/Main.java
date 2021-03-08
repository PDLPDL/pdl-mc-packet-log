package com.pdlpdl.minecraft.log.tool;

import com.pdlpdl.minecraft.log.tool.operation.ReadOperation;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.PrintStream;
import java.io.PrintWriter;

public class Main {

    private ToolOperation operation;

    private String readFilename;
    private boolean compressed;

//========================================
// Main Method
//----------------------------------------

    public static void main(String[] args) {
        Main instance = new Main();
        instance.instanceMain(args);
    }

    public void instanceMain(String[] args) {
        this.parseCommandLine(args);

        if (this.operation == ToolOperation.READ_LOG_FILE) {
            this.executeRead();
        }
    }

//========================================
// Command Line Parsing
//----------------------------------------

    private void parseCommandLine(String[] args) {
        Options options = this.prepareCommandLineOptions();

        CommandLineParser commandLineParser = new DefaultParser();
        try {
            CommandLine commandLine = commandLineParser.parse(options, args);

            if (commandLine.hasOption("r")) {
                this.operation = ToolOperation.READ_LOG_FILE;
                this.readFilename = commandLine.getOptionValue("r");
            }

            if (commandLine.hasOption("z")) {
                this.compressed = true;
            }

            if (this.operation == null) {
                System.err.println("Please choose an operation (-r)");
                this.displayCommandLineHelp(System.err, options);
                System.exit(1);
            }
        } catch (ParseException exc) {
            exc.printStackTrace();

            this.displayCommandLineHelp(System.err, options);
            System.exit(1);
        }
    }

    private Options prepareCommandLineOptions() {
        Options options = new Options();

        options.addOption("h", "help", false, "Display command-line usage");
        options.addOption("r", "read", true, "Read the log file");
        options.addOption("z", "zip", false, "Zip/Unzip log file");

        return options;
    }

    private void displayCommandLineHelp(PrintStream printStream, Options options) {
        HelpFormatter helpFormatter = new HelpFormatter();

        try (PrintWriter printWriter = new PrintWriter(printStream)) {
            helpFormatter.printHelp(printWriter, 120, "Main", null, options, 4, 4, null);
        }
    }

//========================================
// Operations
//----------------------------------------

    private void executeRead() {
        ReadOperation readOperation = new ReadOperation();
        readOperation.setFilename(this.readFilename);
        readOperation.setCompressed(this.compressed);

        readOperation.run();
    }
}
