package de.agentsinaction.tosce;

import picocli.CommandLine;

public class Main {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new TosceCommand())
            .setExecutionExceptionHandler((ex, cmd, parseResult) -> {
                String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                // Flatten multi-line messages for JSON
                String jsonMsg = msg.replace("\\", "\\\\").replace("\"", "'").replace("\n", " | ").replace("\r", "");
                System.err.println("{\"success\":false,\"error\":\"" + jsonMsg + "\"}");
                return 1;
            })
            .execute(args);
        System.exit(exitCode);
    }
}
