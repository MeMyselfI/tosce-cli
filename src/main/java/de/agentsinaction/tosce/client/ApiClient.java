package de.agentsinaction.tosce.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.agentsinaction.tosce.config.Config;

import javax.net.ssl.*;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;

public class ApiClient {

    private final Config config;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public ApiClient(Config config) {
        this.config = config;
        this.mapper = new ObjectMapper();
        this.http = buildHttpClient(config.isInsecure());
    }

    private HttpClient buildHttpClient(boolean insecure) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1);
        if (insecure) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] c, String a) {}
                    public void checkServerTrusted(X509Certificate[] c, String a) {}
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                }}, new SecureRandom());
                builder.sslContext(sslContext);
                builder.sslParameters(insecureSslParams());
            } catch (Exception e) {
                throw new RuntimeException("Failed to configure insecure SSL", e);
            }
        }
        return builder.build();
    }

    private SSLParameters insecureSslParams() {
        SSLParameters p = new SSLParameters();
        p.setEndpointIdentificationAlgorithm("");
        return p;
    }

    private String authHeader() {
        String creds = config.getUser() + ":" + config.getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
    }

    private URI uri(String path) {
        String base = config.getUrl();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return URI.create(base + path);
    }

    public JsonNode get(String path) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri(path))
                .header("Authorization", authHeader())
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp.statusCode(), resp.body());
        return mapper.readTree(resp.body());
    }

    public byte[] getBytes(String path) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri(path))
                .header("Authorization", authHeader())
                .GET()
                .build();
        HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
        checkStatus(resp.statusCode(), "(binary)");
        return resp.body();
    }

    public JsonNode post(String path, Object body) throws IOException, InterruptedException {
        String json = mapper.writeValueAsString(body);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri(path))
                .header("Authorization", authHeader())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp.statusCode(), resp.body());
        return mapper.readTree(resp.body());
    }

    public JsonNode put(String path, Object body) throws IOException, InterruptedException {
        String json = mapper.writeValueAsString(body);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri(path))
                .header("Authorization", authHeader())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp.statusCode(), resp.body());
        return mapper.readTree(resp.body());
    }

    public JsonNode delete(String path) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri(path))
                .header("Authorization", authHeader())
                .header("Accept", "application/json")
                .DELETE()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp.statusCode(), resp.body());
        return mapper.readTree(resp.body());
    }

    public JsonNode postMultipart(String path, Map<String, Object> parts) throws IOException, InterruptedException {
        String boundary = "----ToSceBoundary" + System.currentTimeMillis();
        byte[] body = buildMultipart(boundary, parts);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri(path))
                .header("Authorization", authHeader())
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        // multipart responses may return HTML with embedded JSON or plain JSON
        String responseBody = resp.body();
        // try parsing as JSON directly
        try {
            return mapper.readTree(responseBody);
        } catch (Exception e) {
            // wrap in a simple success node
            return mapper.createObjectNode().put("success", true).put("raw", responseBody);
        }
    }

    public byte[] getBytes(String path, Map<String, String> queryParams) throws IOException, InterruptedException {
        StringBuilder sb = new StringBuilder(path);
        if (queryParams != null && !queryParams.isEmpty()) {
            sb.append("?");
            queryParams.forEach((k, v) -> sb.append(k).append("=").append(v).append("&"));
            sb.deleteCharAt(sb.length() - 1);
        }
        return getBytes(sb.toString());
    }

    private byte[] buildMultipart(String boundary, Map<String, Object> parts) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Map.Entry<String, Object> entry : parts.entrySet()) {
            out.write(("--" + boundary + "\r\n").getBytes());
            if (entry.getValue() instanceof Path filePath) {
                String filename = filePath.getFileName().toString();
                out.write(("Content-Disposition: form-data; name=\"" + entry.getKey()
                        + "\"; filename=\"" + filename + "\"\r\n").getBytes());
                out.write("Content-Type: application/octet-stream\r\n\r\n".getBytes());
                out.write(Files.readAllBytes(filePath));
            } else {
                out.write(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n").getBytes());
                out.write(entry.getValue().toString().getBytes(StandardCharsets.UTF_8));
            }
            out.write("\r\n".getBytes());
        }
        out.write(("--" + boundary + "--\r\n").getBytes());
        return out.toByteArray();
    }

    private void checkStatus(int status, String body) throws IOException {
        if (status == 401) throw new IOException("Authentication failed. Check credentials with 'tosce login'.");
        if (status == 403) throw new IOException("Access denied (HTTP 403).");
        if (status == 404) throw new IOException("Resource not found (HTTP 404).");
        if (status >= 500) throw new IOException("Server error (HTTP " + status + "): " + body);
        if (status < 200 || status >= 300) throw new IOException("Unexpected HTTP " + status + ": " + body);
    }

    public ObjectMapper getMapper() { return mapper; }
}
