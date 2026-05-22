package de.agentsinaction.tosce.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.agentsinaction.tosce.TosceCommand;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(
    name = "examinee",
    description = "Manage examinees (students).",
    mixinStandardHelpOptions = true,
    subcommands = {
        ExamineeCommand.ListCommand.class,
        ExamineeCommand.CreateCommand.class,
        ExamineeCommand.ImportCommand.class,
        ExamineeCommand.DeleteCommand.class,
        HelpCommand.class
    }
)
public class ExamineeCommand implements Callable<Integer> {

    @ParentCommand
    TosceCommand parent;

    @Override
    public Integer call() {
        new CommandLine(this).usage(System.out);
        return 0;
    }

    @Command(name = "list", description = "List examinees. GET /rest/osces/examinees")
    static class ListCommand implements Callable<Integer> {
        @ParentCommand ExamineeCommand cmd;
        @Option(names = {"--osce-id"}, description = "Filter by OSCE ID")
        Long osceId;

        @Override
        public Integer call() throws Exception {
            cmd.parent.requireAuth();
            String path = "/rest/osces/examinees" + (osceId != null ? "?osceID=" + osceId : "");
            JsonNode resp = cmd.parent.resolveClient().get(path);
            cmd.parent.printJson(resp);
            return 0;
        }
    }

    @Command(name = "create", description = "Create a single examinee. POST /rest/osces/examinees")
    static class CreateCommand implements Callable<Integer> {
        @ParentCommand ExamineeCommand cmd;
        @Option(names = {"--osce-id"}, description = "OSCE ID to assign the examinee to", required = true)
        long osceId;
        @Option(names = {"--data"}, description = "Examinee JSON, e.g. '{\"name\":\"Doe\",\"firstname\":\"Jane\"}'", required = true)
        String data;

        @Override
        public Integer call() throws Exception {
            cmd.parent.requireAuth();
            ObjectMapper mapper = cmd.parent.resolveClient().getMapper();
            JsonNode body = mapper.readTree(data);
            JsonNode resp = cmd.parent.resolveClient().post("/rest/osces/examinees?osceID=" + osceId, body);
            cmd.parent.printJson(resp);
            return 0;
        }
    }

    @Command(name = "import", description = "Bulk import examinees from XLS. POST /rest/osces/examinees/xls")
    static class ImportCommand implements Callable<Integer> {
        @ParentCommand ExamineeCommand cmd;
        @Option(names = {"--osce-id"}, description = "OSCE ID", required = true)
        long osceId;
        @Option(names = {"--file", "-f"}, description = "XLS file path", required = true)
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
            parts.put("osceID", String.valueOf(osceId));
            parts.put("method", "import");
            JsonNode resp = cmd.parent.resolveClient().postMultipart("/rest/osces/examinees/xls", parts);
            cmd.parent.printJson(resp);
            return 0;
        }
    }

    @Command(name = "delete", description = "Delete an examinee. DELETE /rest/osces/examinees/{id}")
    static class DeleteCommand implements Callable<Integer> {
        @ParentCommand ExamineeCommand cmd;
        @Parameters(paramLabel = "ID", description = "Examinee ID")
        long id;

        @Override
        public Integer call() throws Exception {
            cmd.parent.requireAuth();
            JsonNode resp = cmd.parent.resolveClient().delete("/rest/osces/examinees/" + id);
            cmd.parent.printJson(resp);
            return 0;
        }
    }
}
