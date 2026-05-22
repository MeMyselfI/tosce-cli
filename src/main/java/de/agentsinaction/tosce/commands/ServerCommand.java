package de.agentsinaction.tosce.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.agentsinaction.tosce.TosceCommand;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.util.concurrent.Callable;

@Command(
    name = "server",
    description = "Server management commands.",
    mixinStandardHelpOptions = true,
    subcommands = {
        ServerCommand.InfoCommand.class,
        ServerCommand.LetsencryptCommand.class,
        HelpCommand.class
    }
)
public class ServerCommand implements Callable<Integer> {

    @ParentCommand
    TosceCommand parent;

    @Override
    public Integer call() {
        new CommandLine(this).usage(System.out);
        return 0;
    }

    @Command(name = "info", description = "Show server info and version. GET /rest/conf")
    static class InfoCommand implements Callable<Integer> {
        @ParentCommand ServerCommand cmd;

        @Override
        public Integer call() throws Exception {
            cmd.parent.requireAuth();
            JsonNode resp = cmd.parent.resolveClient().get("/rest/conf");
            // Extract relevant server info fields
            ObjectNode info = cmd.parent.resolveClient().getMapper().createObjectNode();
            if (resp.has("root") && resp.get("root").isArray() && !resp.get("root").isEmpty()) {
                JsonNode root = resp.get("root").get(0);
                if (root.has("version")) info.set("version", root.get("version"));
                if (root.has("serverName")) info.set("serverName", root.get("serverName"));
                if (root.has("port")) info.set("port", root.get("port"));
                if (root.has("httpsPort")) info.set("httpsPort", root.get("httpsPort"));
            } else {
                info.set("raw", resp);
            }
            info.put("url", cmd.parent.resolveConfig().getUrl());
            cmd.parent.printJson(info);
            return 0;
        }
    }

    @Command(name = "letsencrypt", description = "Refresh Let's Encrypt certificate. POST /rest/server/letsencrypt/refresh")
    static class LetsencryptCommand implements Callable<Integer> {
        @ParentCommand ServerCommand cmd;

        @Override
        public Integer call() throws Exception {
            cmd.parent.requireAuth();
            JsonNode resp = cmd.parent.resolveClient().post("/rest/server/letsencrypt/refresh", "{}");
            cmd.parent.printJson(resp);
            return 0;
        }
    }
}
