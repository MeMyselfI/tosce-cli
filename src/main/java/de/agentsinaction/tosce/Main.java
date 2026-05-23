package de.agentsinaction.tosce;

import de.agentsinaction.tosce.gui.GuiServer;
import picocli.CommandLine;

public class Main {
    public static void main(String[] args) {
        // No terminal attached and no arguments → launched by double-click → GUI mode
        if (System.console() == null && args.length == 0) {
            String exe = ProcessHandle.current().info().command().orElse("");
            try {
                GuiServer.start(exe);
            } catch (Exception e) {
                System.err.println("{\"success\":false,\"error\":\"GUI failed: " + e.getMessage() + "\"}");
                System.exit(1);
            }
            return;
        }

        int exitCode = new CommandLine(new TosceCommand())
            .setExecutionExceptionHandler((ex, cmd, parseResult) -> {
                String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                String jsonMsg = msg.replace("\\", "\\\\").replace("\"", "'").replace("\n", " | ").replace("\r", "");
                System.err.println("{\"success\":false,\"error\":\"" + jsonMsg + "\"}");
                return 1;
            })
            .execute(args);
        System.exit(exitCode);
    }
}
