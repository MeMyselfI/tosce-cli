package de.agentsinaction.tosce;

import picocli.CommandLine;

public class Main {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new TosceCommand()).execute(args);
        System.exit(exitCode);
    }
}
