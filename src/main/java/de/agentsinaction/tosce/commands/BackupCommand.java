package de.agentsinaction.tosce.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.agentsinaction.tosce.TosceCommand;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(
    name = "backup",
    description = "Backup and restore OSCE data.",
    mixinStandardHelpOptions = true,
    subcommands = {
        BackupCommand.DumpCommand.class,
        BackupCommand.RestoreCommand.class,
        HelpCommand.class
    }
)
public class BackupCommand implements Callable<Integer> {

    @ParentCommand
    TosceCommand parent;

    @Override
    public Integer call() {
        new CommandLine(this).usage(System.out);
        return 0;
    }

    @Command(name = "dump", description = "Download database dump as ZIP. GET /rest/Osce/{id}/db/dump")
    static class DumpCommand implements Callable<Integer> {
        @ParentCommand BackupCommand cmd;
        @Parameters(paramLabel = "OSCE_ID") long osceId;
        @Option(names = {"--out", "-o"}, description = "Output ZIP file")
        Path out;

        @Override
        public Integer call() throws Exception {
            cmd.parent.requireAuth();
            byte[] data = cmd.parent.resolveClient().getBytes("/rest/Osce/" + osceId + "/db/dump");
            Path target = out != null ? out : Path.of("backup-osce-" + osceId + ".zip");
            Files.write(target, data);
            ObjectNode result = cmd.parent.resolveClient().getMapper().createObjectNode();
            result.put("success", true);
            result.put("file", target.toAbsolutePath().toString());
            result.put("bytes", data.length);
            cmd.parent.printJson(result);
            return 0;
        }
    }

    @Command(name = "restore", description = "Restore from backup file. POST /rest/Osce/restore")
    static class RestoreCommand implements Callable<Integer> {
        @ParentCommand BackupCommand cmd;
        @Option(names = {"--file", "-f"}, description = "Backup file (.backup or .zip)", required = true)
        Path file;

        @Override
        public Integer call() throws Exception {
            cmd.parent.requireAuth();
            if (!Files.exists(file)) {
                System.err.println("{\"success\":false,\"error\":\"File not found: " + file + "\"}");
                return 1;
            }
            Map<String, Object> parts = new LinkedHashMap<>();
            parts.put("file", file);
            JsonNode resp = cmd.parent.resolveClient().postMultipart("/rest/Osce/restore", parts);
            cmd.parent.printJson(resp);
            return 0;
        }
    }
}
