package de.agentsinaction.tosce.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.agentsinaction.tosce.TosceCommand;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.util.concurrent.Callable;

@Command(
    name = "actor",
    description = "Manage actors (standardized patients).",
    mixinStandardHelpOptions = true,
    subcommands = {
        ActorCommand.CreateCommand.class,
        ActorCommand.UpdateCommand.class,
        ActorCommand.DeleteCommand.class,
        HelpCommand.class
    }
)
public class ActorCommand implements Callable<Integer> {

    @ParentCommand
    TosceCommand parent;

    @Override
    public Integer call() {
        new CommandLine(this).usage(System.out);
        return 0;
    }

    @Command(name = "create", description = "Create an actor. POST /rest/osces/actors")
    static class CreateCommand implements Callable<Integer> {
        @ParentCommand ActorCommand cmd;
        @Option(names = {"--data"}, description = "Actor JSON", required = true)
        String data;

        @Override
        public Integer call() throws Exception {
            cmd.parent.requireAuth();
            ObjectMapper mapper = cmd.parent.resolveClient().getMapper();
            JsonNode body = mapper.readTree(data);
            JsonNode resp = cmd.parent.resolveClient().post("/rest/osces/actors", body);
            cmd.parent.printJson(resp);
            return 0;
        }
    }

    @Command(name = "update", description = "Update an actor. PUT /rest/osces/actors/{id}")
    static class UpdateCommand implements Callable<Integer> {
        @ParentCommand ActorCommand cmd;
        @Parameters(paramLabel = "ID") long id;
        @Option(names = {"--data"}, description = "Actor JSON", required = true)
        String data;

        @Override
        public Integer call() throws Exception {
            cmd.parent.requireAuth();
            ObjectMapper mapper = cmd.parent.resolveClient().getMapper();
            JsonNode body = mapper.readTree(data);
            JsonNode resp = cmd.parent.resolveClient().put("/rest/osces/actors/" + id, body);
            cmd.parent.printJson(resp);
            return 0;
        }
    }

    @Command(name = "delete", description = "Delete an actor. DELETE /rest/osces/actors/{id}")
    static class DeleteCommand implements Callable<Integer> {
        @ParentCommand ActorCommand cmd;
        @Parameters(paramLabel = "ID") long id;

        @Override
        public Integer call() throws Exception {
            cmd.parent.requireAuth();
            JsonNode resp = cmd.parent.resolveClient().delete("/rest/osces/actors/" + id);
            cmd.parent.printJson(resp);
            return 0;
        }
    }
}
