package de.agentsinaction.tosce.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.agentsinaction.tosce.TosceCommand;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.util.concurrent.Callable;

@Command(
    name = "user",
    description = "Manage backend users (admins/examiners).",
    mixinStandardHelpOptions = true,
    subcommands = {
        UserCommand.ListCommand.class,
        UserCommand.CreateCommand.class,
        UserCommand.UpdateCommand.class,
        UserCommand.DeleteCommand.class,
        HelpCommand.class
    }
)
public class UserCommand implements Callable<Integer> {

    @ParentCommand
    TosceCommand parent;

    @Override
    public Integer call() {
        new CommandLine(this).usage(System.out);
        return 0;
    }

    @Command(name = "list", description = "List all backend users. GET /rest/Backenduser")
    static class ListCommand implements Callable<Integer> {
        @ParentCommand UserCommand cmd;

        @Override
        public Integer call() throws Exception {
            cmd.parent.requireAuth();
            JsonNode resp = cmd.parent.resolveClient().get("/rest/Backenduser");
            cmd.parent.printJson(resp);
            return 0;
        }
    }

    @Command(name = "create", description = "Create a backend user. POST /rest/Backenduser/{id}")
    static class CreateCommand implements Callable<Integer> {
        @ParentCommand UserCommand cmd;
        @Parameters(paramLabel = "ID", description = "User ID (numeric or username)")
        String id;
        @Option(names = {"--data"}, description = "User JSON", required = true)
        String data;

        @Override
        public Integer call() throws Exception {
            cmd.parent.requireAuth();
            ObjectMapper mapper = cmd.parent.resolveClient().getMapper();
            JsonNode body = mapper.readTree(data);
            JsonNode resp = cmd.parent.resolveClient().post("/rest/Backenduser/" + id, body);
            cmd.parent.printJson(resp);
            return 0;
        }
    }

    @Command(name = "update", description = "Update a backend user. PUT /rest/Backenduser/{id}")
    static class UpdateCommand implements Callable<Integer> {
        @ParentCommand UserCommand cmd;
        @Parameters(paramLabel = "ID") String id;
        @Option(names = {"--data"}, description = "User JSON", required = true)
        String data;

        @Override
        public Integer call() throws Exception {
            cmd.parent.requireAuth();
            ObjectMapper mapper = cmd.parent.resolveClient().getMapper();
            JsonNode body = mapper.readTree(data);
            JsonNode resp = cmd.parent.resolveClient().put("/rest/Backenduser/" + id, body);
            cmd.parent.printJson(resp);
            return 0;
        }
    }

    @Command(name = "delete", description = "Delete a backend user. DELETE /rest/Backenduser/{id}")
    static class DeleteCommand implements Callable<Integer> {
        @ParentCommand UserCommand cmd;
        @Parameters(paramLabel = "ID") String id;

        @Override
        public Integer call() throws Exception {
            cmd.parent.requireAuth();
            JsonNode resp = cmd.parent.resolveClient().delete("/rest/Backenduser/" + id);
            cmd.parent.printJson(resp);
            return 0;
        }
    }
}
