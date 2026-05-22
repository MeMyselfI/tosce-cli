package de.agentsinaction.tosce.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.agentsinaction.tosce.TosceCommand;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.util.concurrent.Callable;

@Command(
    name = "examiner",
    description = "Manage examiners (raters).",
    mixinStandardHelpOptions = true,
    subcommands = {
        ExaminerCommand.ListCommand.class,
        ExaminerCommand.CreateCommand.class,
        ExaminerCommand.UpdateCommand.class,
        ExaminerCommand.DeleteCommand.class,
        HelpCommand.class
    }
)
public class ExaminerCommand implements Callable<Integer> {

    @ParentCommand
    TosceCommand parent;

    @Override
    public Integer call() {
        new CommandLine(this).usage(System.out);
        return 0;
    }

    @Command(name = "list", description = "List all distinct examiners. GET /rest/examiners")
    static class ListCommand implements Callable<Integer> {
        @ParentCommand ExaminerCommand cmd;

        @Override
        public Integer call() throws Exception {
            cmd.parent.requireAuth();
            JsonNode resp = cmd.parent.resolveClient().get("/rest/examiners");
            cmd.parent.printJson(resp);
            return 0;
        }
    }

    @Command(name = "create", description = "Create an examiner. POST /rest/osces/examiners")
    static class CreateCommand implements Callable<Integer> {
        @ParentCommand ExaminerCommand cmd;
        @Option(names = {"--data"}, description = "Examiner JSON", required = true)
        String data;

        @Override
        public Integer call() throws Exception {
            cmd.parent.requireAuth();
            ObjectMapper mapper = cmd.parent.resolveClient().getMapper();
            JsonNode body = mapper.readTree(data);
            JsonNode resp = cmd.parent.resolveClient().post("/rest/osces/examiners", body);
            cmd.parent.printJson(resp);
            return 0;
        }
    }

    @Command(name = "update", description = "Update an examiner. PUT /rest/osces/examiners/{id}")
    static class UpdateCommand implements Callable<Integer> {
        @ParentCommand ExaminerCommand cmd;
        @Parameters(paramLabel = "ID") long id;
        @Option(names = {"--data"}, description = "Examiner JSON", required = true)
        String data;

        @Override
        public Integer call() throws Exception {
            cmd.parent.requireAuth();
            ObjectMapper mapper = cmd.parent.resolveClient().getMapper();
            JsonNode body = mapper.readTree(data);
            JsonNode resp = cmd.parent.resolveClient().put("/rest/osces/examiners/" + id, body);
            cmd.parent.printJson(resp);
            return 0;
        }
    }

    @Command(name = "delete", description = "Delete an examiner. DELETE /rest/osces/examiners/{id}")
    static class DeleteCommand implements Callable<Integer> {
        @ParentCommand ExaminerCommand cmd;
        @Parameters(paramLabel = "ID") long id;

        @Override
        public Integer call() throws Exception {
            cmd.parent.requireAuth();
            JsonNode resp = cmd.parent.resolveClient().delete("/rest/osces/examiners/" + id);
            cmd.parent.printJson(resp);
            return 0;
        }
    }
}
