package de.agentsinaction.tosce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.agentsinaction.tosce.client.ApiClient;
import de.agentsinaction.tosce.commands.*;
import de.agentsinaction.tosce.config.Config;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.util.concurrent.Callable;

@Command(
    name = "tosce",
    version = "tosce-cli 1.0.0",
    mixinStandardHelpOptions = true,
    description = "CLI for the tOSCE OSCE exam server. Output is JSON by default.",
    subcommands = {
        LoginCommand.class,
        OsceCommand.class,
        ExamineeCommand.class,
        ExaminerCommand.class,
        ActorCommand.class,
        QrCommand.class,
        BackupCommand.class,
        UserCommand.class,
        ClientCommand.class,
        ConfCommand.class,
        ServerCommand.class,
        CommandLine.HelpCommand.class
    }
)
public class TosceCommand implements Callable<Integer> {

    @Option(names = {"--url"}, description = "Server URL (overrides config/env)", scope = ScopeType.INHERIT)
    public String url;

    @Option(names = {"--user", "-u"}, description = "Username (overrides config/env)", scope = ScopeType.INHERIT)
    public String user;

    @Option(names = {"--pass", "-p"}, description = "Password (overrides config/env)", scope = ScopeType.INHERIT, interactive = false)
    public String pass;

    @Option(names = {"--insecure", "-k"}, description = "Skip TLS certificate verification", scope = ScopeType.INHERIT)
    public Boolean insecure;

    @Option(names = {"--pretty"}, description = "Pretty-print JSON output", scope = ScopeType.INHERIT)
    public boolean pretty;

    private Config config;
    private ApiClient apiClient;

    public Config resolveConfig() {
        if (config == null) {
            config = Config.load();
            config.applyOverrides(url, user, pass, insecure);
        }
        return config;
    }

    public ApiClient resolveClient() {
        if (apiClient == null) {
            apiClient = new ApiClient(resolveConfig());
        }
        return apiClient;
    }

    public void printJson(JsonNode node) {
        try {
            ObjectMapper mapper = resolveClient().getMapper();
            if (pretty) {
                System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node));
            } else {
                System.out.println(mapper.writeValueAsString(node));
            }
        } catch (Exception e) {
            System.err.println("Error serializing output: " + e.getMessage());
        }
    }

    public void printJson(Object obj) {
        try {
            ObjectMapper mapper = resolveClient().getMapper();
            if (pretty) {
                System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj));
            } else {
                System.out.println(mapper.writeValueAsString(obj));
            }
        } catch (Exception e) {
            System.err.println("Error serializing output: " + e.getMessage());
        }
    }

    public void requireAuth() {
        Config cfg = resolveConfig();
        if (!cfg.isValid()) {
            throw new ParameterException(new CommandLine(this),
                "Not logged in. Run: tosce login --url <url> --user <user> --pass <password>");
        }
    }

    @Override
    public Integer call() {
        new CommandLine(this).usage(System.out);
        return 0;
    }
}
