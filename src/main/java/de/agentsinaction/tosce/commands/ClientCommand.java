package de.agentsinaction.tosce.commands;

import com.fasterxml.jackson.databind.JsonNode;
import de.agentsinaction.tosce.TosceCommand;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.util.concurrent.Callable;

@Command(
    name = "client",
    description = "Manage connected devices/clients.",
    mixinStandardHelpOptions = true,
    subcommands = {
        ClientCommand.ListCommand.class,
        HelpCommand.class
    }
)
public class ClientCommand implements Callable<Integer> {

    @ParentCommand
    TosceCommand parent;

    @Override
    public Integer call() {
        new CommandLine(this).usage(System.out);
        return 0;
    }

    @Command(name = "list", description = "List all connected clients/devices. GET /rest/clients")
    static class ListCommand implements Callable<Integer> {
        @ParentCommand ClientCommand cmd;

        @Override
        public Integer call() throws Exception {
            cmd.parent.requireAuth();
            JsonNode resp = cmd.parent.resolveClient().get("/rest/clients");
            cmd.parent.printJson(resp);
            return 0;
        }
    }
}
