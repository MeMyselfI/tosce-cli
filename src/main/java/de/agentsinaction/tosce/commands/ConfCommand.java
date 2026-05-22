package de.agentsinaction.tosce.commands;

import com.fasterxml.jackson.databind.JsonNode;
import de.agentsinaction.tosce.TosceCommand;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.util.concurrent.Callable;

@Command(
    name = "conf",
    description = "Show server configuration.",
    mixinStandardHelpOptions = true,
    subcommands = {
        ConfCommand.GetCommand.class,
        HelpCommand.class
    }
)
public class ConfCommand implements Callable<Integer> {

    @ParentCommand
    TosceCommand parent;

    @Override
    public Integer call() {
        new CommandLine(this).usage(System.out);
        return 0;
    }

    @Command(name = "get", description = "Get server configuration. GET /rest/conf")
    static class GetCommand implements Callable<Integer> {
        @ParentCommand ConfCommand cmd;

        @Override
        public Integer call() throws Exception {
            cmd.parent.requireAuth();
            JsonNode resp = cmd.parent.resolveClient().get("/rest/conf");
            cmd.parent.printJson(resp);
            return 0;
        }
    }
}
