package de.agentsinaction.tosce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.agentsinaction.tosce.client.ApiClient;
import de.agentsinaction.tosce.commands.*;
import de.agentsinaction.tosce.config.Config;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "tosce",
    version = "tosce-cli 1.0.0-beta",
    mixinStandardHelpOptions = true,
    description = {"CLI for the tOSCE OSCE exam server. Output is JSON by default.",
                   "@|bold,red WARNING: BETA SOFTWARE — USE AT YOUR OWN RISK.|@",
                   "May cause data loss. Not for use in production without testing."},
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

    @Option(names = {"--table"}, description = "Render root array as a table (human-readable)", scope = ScopeType.INHERIT)
    public boolean table;

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
        if (table) {
            printTable(node);
            return;
        }
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
            if (table) {
                printTable(mapper.valueToTree(obj));
                return;
            }
            if (pretty) {
                System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj));
            } else {
                System.out.println(mapper.writeValueAsString(obj));
            }
        } catch (Exception e) {
            System.err.println("Error serializing output: " + e.getMessage());
        }
    }

    private void printTable(JsonNode node) {
        // Unwrap standard ResultWrapper: use root array if present
        JsonNode rows = node;
        if (node.has("root") && node.get("root").isArray()) {
            rows = node.get("root");
        }

        if (!rows.isArray() || rows.isEmpty()) {
            // Not a list — fall back to pretty JSON
            try {
                System.out.println(resolveClient().getMapper()
                    .writerWithDefaultPrettyPrinter().writeValueAsString(node));
            } catch (Exception e) {
                System.err.println("Error serializing output: " + e.getMessage());
            }
            return;
        }

        // Collect column names from first row
        List<String> cols = new ArrayList<>();
        rows.get(0).fieldNames().forEachRemaining(cols::add);

        // Calculate column widths
        int[] widths = new int[cols.size()];
        for (int i = 0; i < cols.size(); i++) widths[i] = cols.get(i).length();
        for (JsonNode row : rows) {
            for (int i = 0; i < cols.size(); i++) {
                JsonNode cell = row.get(cols.get(i));
                int len = cell == null ? 0 : cellValue(cell).length();
                if (len > widths[i]) widths[i] = Math.min(len, 60);
            }
        }

        // Print header
        printTableRow(cols.stream().map(String::toUpperCase).toList(), widths);
        printTableSeparator(widths);

        // Print rows
        for (JsonNode row : rows) {
            List<String> cells = new ArrayList<>();
            for (String col : cols) {
                JsonNode cell = row.get(col);
                String val = cell == null ? "" : cellValue(cell);
                cells.add(val.length() > 60 ? val.substring(0, 57) + "..." : val);
            }
            printTableRow(cells, widths);
        }
    }

    private String cellValue(JsonNode node) {
        if (node.isNull()) return "";
        if (node.isTextual()) return node.asText();
        if (node.isNumber()) return node.asText();
        if (node.isBoolean()) return node.asText();
        return node.toString();
    }

    private void printTableRow(List<String> cells, int[] widths) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            sb.append("| ").append(pad(cells.get(i), widths[i])).append(" ");
        }
        sb.append("|");
        System.out.println(sb);
    }

    private void printTableSeparator(int[] widths) {
        StringBuilder sb = new StringBuilder();
        for (int w : widths) sb.append("+").append("-".repeat(w + 2));
        sb.append("+");
        System.out.println(sb);
    }

    private String pad(String s, int width) {
        if (s.length() >= width) return s;
        return s + " ".repeat(width - s.length());
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
