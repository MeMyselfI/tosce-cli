package de.agentsinaction.tosce.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.agentsinaction.tosce.TosceCommand;
import de.agentsinaction.tosce.client.ApiClient;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.io.*;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(
    name = "osce",
    description = "Manage OSCEs (Objective Structured Clinical Examinations).",
    mixinStandardHelpOptions = true,
    subcommands = {
        OsceCommand.ListCommand.class,
        OsceCommand.GetCommand.class,
        OsceCommand.CreateCommand.class,
        OsceCommand.DeleteCommand.class,
        OsceCommand.ExportCommand.class,
        OsceCommand.ResultCommand.class,
        OsceCommand.PhrasesCommand.class,
        HelpCommand.class
    }
)
public class OsceCommand implements Callable<Integer> {

    @ParentCommand
    TosceCommand parent;

    @Override
    public Integer call() {
        new CommandLine(this).usage(System.out);
        return 0;
    }

    @Command(name = "list", description = "List all OSCEs. GET /rest/osces")
    static class ListCommand implements Callable<Integer> {
        @ParentCommand OsceCommand osce;
        @Option(names = {"--current"}, description = "Show only currently active OSCEs")
        boolean current;

        @Override
        public Integer call() throws Exception {
            osce.parent.requireAuth();
            String path = "/rest/osces" + (current ? "?current=true" : "");
            JsonNode resp = osce.parent.resolveClient().get(path);
            osce.parent.printJson(resp);
            return 0;
        }
    }

    @Command(name = "get", description = "Get details of a single OSCE. GET /rest/osces/{id}")
    static class GetCommand implements Callable<Integer> {
        @ParentCommand OsceCommand osce;
        @Parameters(paramLabel = "ID", description = "OSCE ID")
        long id;

        @Override
        public Integer call() throws Exception {
            osce.parent.requireAuth();
            JsonNode resp = osce.parent.resolveClient().get("/rest/osces/" + id);
            osce.parent.printJson(resp);
            return 0;
        }
    }

    @Command(name = "create", description = "Import/create an OSCE from XML or ZIP. POST /rest/osces")
    static class CreateCommand implements Callable<Integer> {
        @ParentCommand OsceCommand osce;
        @Option(names = {"--file", "-f"}, description = "XML or ZIP file to import", required = true)
        Path file;
        @Option(names = {"--lang"}, description = "Language (de, en)", defaultValue = "de")
        String lang;

        @Override
        public Integer call() throws Exception {
            osce.parent.requireAuth();
            if (!Files.exists(file)) {
                System.err.println("{\"success\":false,\"error\":\"File not found: " + file + "\"}");
                return 1;
            }
            Map<String, Object> parts = new LinkedHashMap<>();
            parts.put("file", file);
            parts.put("lang", lang);
            parts.put("method", "import");
            JsonNode resp = osce.parent.resolveClient().postMultipart("/rest/osces", parts);
            osce.parent.printJson(resp);
            return 0;
        }
    }

    @Command(name = "delete", description = "Delete an OSCE. DELETE /rest/osces/{id}")
    static class DeleteCommand implements Callable<Integer> {
        @ParentCommand OsceCommand osce;
        @Parameters(paramLabel = "ID", description = "OSCE ID")
        long id;

        @Override
        public Integer call() throws Exception {
            osce.parent.requireAuth();
            JsonNode resp = osce.parent.resolveClient().delete("/rest/osces/" + id);
            osce.parent.printJson(resp);
            return 0;
        }
    }

    @Command(name = "export", description = "Export OSCE as XML, HTML or PDF. GET /rest/osces/{id}/{format}")
    static class ExportCommand implements Callable<Integer> {
        @ParentCommand OsceCommand osce;
        @Parameters(paramLabel = "ID", description = "OSCE ID")
        long id;
        @Option(names = {"--format", "-f"}, description = "Format: xml, html, pdf", required = true)
        String format;
        @Option(names = {"--lang"}, description = "Language (de, en)", defaultValue = "de")
        String lang;
        @Option(names = {"--out", "-o"}, description = "Output file path")
        Path out;

        @Override
        public Integer call() throws Exception {
            osce.parent.requireAuth();
            String path = "/rest/osces/" + id + "/" + format + "?lang=" + lang;
            byte[] data = osce.parent.resolveClient().getBytes(path);
            Path target = out != null ? out : Path.of("osce-" + id + "." + format);
            Files.write(target, data);
            ObjectNode result = osce.parent.resolveClient().getMapper().createObjectNode();
            result.put("success", true);
            result.put("file", target.toAbsolutePath().toString());
            result.put("bytes", data.length);
            osce.parent.printJson(result);
            return 0;
        }
    }

    @Command(name = "result", description = "Download OSCE results in various formats.")
    static class ResultCommand implements Callable<Integer> {
        @ParentCommand OsceCommand osce;
        @Parameters(paramLabel = "ID", description = "OSCE ID")
        long id;
        @Option(names = {"--format", "-f"},
                description = "Format: xlsx (examiner), pdf (feedback), ims-xml, feedback-pdf-zip, xml",
                required = true)
        String format;
        @Option(names = {"--lang"}, description = "Language (de, en)", defaultValue = "de")
        String lang;
        @Option(names = {"--out", "-o"}, description = "Output file path")
        Path out;

        @Override
        public Integer call() throws Exception {
            osce.parent.requireAuth();
            String path;
            String ext;
            switch (format.toLowerCase()) {
                case "xlsx" -> { path = "/rest/osces/" + id + "/result/examiner/xlsx?lang=" + lang; ext = "xlsx"; }
                case "pdf", "feedback-pdf" -> { path = "/rest/osces/" + id + "/result/feedback/pdf?lang=" + lang; ext = "pdf"; }
                case "feedback-pdf-zip" -> { path = "/rest/osces/" + id + "/result/feedback/pdf/zip?lang=" + lang; ext = "zip"; }
                case "ims-xml" -> { path = "/rest/osces/" + id + "/result/ims/xml"; ext = "xml"; }
                case "xml" -> { path = "/rest/osces/" + id + "/result/xml"; ext = "xml"; }
                default -> {
                    System.err.println("{\"success\":false,\"error\":\"Unknown format: " + format + "\"}");
                    return 1;
                }
            }
            byte[] data = osce.parent.resolveClient().getBytes(path);
            Path target = out != null ? out : Path.of("result-" + id + "." + ext);
            Files.write(target, data);
            ObjectNode result = osce.parent.resolveClient().getMapper().createObjectNode();
            result.put("success", true);
            result.put("file", target.toAbsolutePath().toString());
            result.put("bytes", data.length);
            osce.parent.printJson(result);
            return 0;
        }
    }

    @Command(name = "phrases", description = "Manage OSCE phrases.",
             subcommands = {OsceCommand.PhrasesCommand.ListPhrases.class,
                            OsceCommand.PhrasesCommand.ImportPhrases.class, HelpCommand.class})
    static class PhrasesCommand implements Callable<Integer> {
        @ParentCommand OsceCommand osce;

        @Override
        public Integer call() {
            new CommandLine(this).usage(System.out);
            return 0;
        }

        @Command(name = "list", description = "List phrases tree. GET /rest/osces/{id}/phrases/root")
        static class ListPhrases implements Callable<Integer> {
            @ParentCommand PhrasesCommand phrases;
            @Parameters(paramLabel = "OSCE_ID") long id;

            @Override
            public Integer call() throws Exception {
                phrases.osce.parent.requireAuth();
                JsonNode resp = phrases.osce.parent.resolveClient().get("/rest/osces/" + id + "/phrases/root");
                phrases.osce.parent.printJson(resp);
                return 0;
            }
        }

        @Command(name = "import", description = "Import phrases from XLSX. POST /rest/osces/{id}/phrases")
        static class ImportPhrases implements Callable<Integer> {
            @ParentCommand PhrasesCommand phrases;
            @Parameters(paramLabel = "OSCE_ID") long id;
            @Option(names = {"--file", "-f"}, required = true) Path file;

            @Override
            public Integer call() throws Exception {
                phrases.osce.parent.requireAuth();
                Map<String, Object> parts = new LinkedHashMap<>();
                parts.put("file", file);
                parts.put("method", "import");
                JsonNode resp = phrases.osce.parent.resolveClient().postMultipart("/rest/osces/" + id + "/phrases", parts);
                phrases.osce.parent.printJson(resp);
                return 0;
            }
        }
    }
}
