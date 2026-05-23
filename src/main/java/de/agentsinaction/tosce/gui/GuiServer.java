package de.agentsinaction.tosce.gui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.agentsinaction.tosce.config.Config;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class GuiServer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void start(String exePath) throws Exception {
        int port;
        try (ServerSocket ss = new ServerSocket(0)) {
            port = ss.getLocalPort();
        }

        HttpServer server = HttpServer.create(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        server.createContext("/", ex -> {
            if (!"/".equals(ex.getRequestURI().getPath())) { respond(ex, 404, "{}"); return; }
            serveHtml(ex);
        });
        server.createContext("/config",      ex -> serveConfig(ex));
        server.createContext("/run",          ex -> handleRun(ex, exePath));
        server.createContext("/save-config",  ex -> handleSaveConfig(ex));
        server.createContext("/shutdown",     ex -> {
            respond(ex, 200, "{\"ok\":true}");
            server.stop(0);
            System.exit(0);
        });

        server.start();
        openBrowser("http://localhost:" + port);

        Thread.currentThread().join();
    }

    private static void serveHtml(HttpExchange ex) throws IOException {
        try (InputStream is = GuiServer.class.getResourceAsStream("/gui.html")) {
            byte[] bytes = is != null ? is.readAllBytes() : "<h1>gui.html not found</h1>".getBytes();
            ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
        }
        ex.getResponseBody().close();
    }

    private static void serveConfig(HttpExchange ex) throws IOException {
        Config cfg = Config.load();
        ObjectNode node = MAPPER.createObjectNode()
                .put("url",      cfg.getUrl() != null      ? cfg.getUrl()      : "")
                .put("user",     cfg.getUser() != null     ? cfg.getUser()     : "")
                .put("insecure", cfg.isInsecure());
        respond(ex, 200, node.toString());
    }

    private static void handleRun(HttpExchange ex, String exePath) throws IOException {
        JsonNode req = MAPPER.readTree(ex.getRequestBody().readAllBytes());

        List<String> args = new ArrayList<>();
        args.add(exePath);
        addArg(args, "--url",  req, "url");
        addArg(args, "--user", req, "user");
        addArg(args, "--pass", req, "pass");
        if (req.path("insecure").asBoolean(false)) args.add("--insecure");
        args.add("--pretty");

        String command = req.path("command").asText("").trim();
        args.addAll(splitArgs(command));

        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        String output = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = -1;
        try { exitCode = proc.waitFor(); } catch (InterruptedException ignored) {}

        ObjectNode result = MAPPER.createObjectNode()
                .put("output",   output)
                .put("exitCode", exitCode);
        respond(ex, 200, result.toString());
    }

    private static void handleSaveConfig(HttpExchange ex) throws IOException {
        JsonNode req = MAPPER.readTree(ex.getRequestBody().readAllBytes());
        Config cfg = Config.load();
        cfg.setUrl(req.path("url").asText(""));
        cfg.setUser(req.path("user").asText(""));
        cfg.setPassword(req.path("pass").asText(""));
        cfg.setInsecure(req.path("insecure").asBoolean(false));
        try {
            cfg.save();
            respond(ex, 200, "{\"ok\":true}");
        } catch (Exception e) {
            respond(ex, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private static void addArg(List<String> args, String flag, JsonNode req, String field) {
        String val = req.path(field).asText("").trim();
        if (!val.isEmpty()) args.add(flag + "=" + val);
    }

    private static List<String> splitArgs(String s) {
        List<String> result = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        char quote = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quote != 0) {
                if (c == quote) quote = 0;
                else cur.append(c);
            } else if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == ' ' || c == '\t') {
                if (cur.length() > 0) { result.add(cur.toString()); cur.setLength(0); }
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) result.add(cur.toString());
        return result;
    }

    private static void respond(HttpExchange ex, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(code, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    private static void openBrowser(String url) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("win"))       pb = new ProcessBuilder("cmd", "/c", "start", "", url);
            else if (os.contains("mac"))  pb = new ProcessBuilder("open", url);
            else                          pb = new ProcessBuilder("xdg-open", url);
            pb.start();
        } catch (Exception ignored) {
            System.out.println("Open in browser: " + url);
        }
    }
}
