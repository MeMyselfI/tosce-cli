package de.agentsinaction.tosce.config;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class Config {

    private static final Path CONFIG_DIR = Path.of(System.getProperty("user.home"), ".tosce");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config");

    private String url;
    private String user;
    private String password;
    private boolean insecure;

    private Config() {}

    public static Config load() {
        Config c = new Config();
        Properties props = new Properties();
        if (Files.exists(CONFIG_FILE)) {
            try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
                props.load(in);
            } catch (IOException ignored) {}
        }
        // Priority 3: config file
        c.url = props.getProperty("url", "");
        c.user = props.getProperty("user", "");
        c.password = props.getProperty("password", "");
        c.insecure = Boolean.parseBoolean(props.getProperty("insecure", "false"));

        // Priority 2: environment variables (override config file)
        String envUrl = System.getenv("TOSCE_URL");
        String envUser = System.getenv("TOSCE_USER");
        String envPass = System.getenv("TOSCE_PASSWORD");
        String envInsecure = System.getenv("TOSCE_INSECURE");
        if (envUrl != null && !envUrl.isBlank()) c.url = envUrl;
        if (envUser != null && !envUser.isBlank()) c.user = envUser;
        if (envPass != null && !envPass.isBlank()) c.password = envPass;
        if (envInsecure != null) c.insecure = Boolean.parseBoolean(envInsecure);

        return c;
    }

    public void save() throws IOException {
        Files.createDirectories(CONFIG_DIR);
        Properties props = new Properties();
        props.setProperty("url", url != null ? url : "");
        props.setProperty("user", user != null ? user : "");
        props.setProperty("password", password != null ? password : "");
        props.setProperty("insecure", String.valueOf(insecure));
        try (OutputStream out = Files.newOutputStream(CONFIG_FILE,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            props.store(out, "tOSCE CLI configuration");
        }
        // Restrict permissions on Unix
        try {
            CONFIG_FILE.toFile().setReadable(false, false);
            CONFIG_FILE.toFile().setReadable(true, true);
            CONFIG_FILE.toFile().setWritable(false, false);
            CONFIG_FILE.toFile().setWritable(true, true);
        } catch (Exception ignored) {}
    }

    public String getUrl() { return url; }
    public String getUser() { return user; }
    public String getPassword() { return password; }
    public boolean isInsecure() { return insecure; }

    public void setUrl(String url) { this.url = url; }
    public void setUser(String user) { this.user = user; }
    public void setPassword(String password) { this.password = password; }
    public void setInsecure(boolean insecure) { this.insecure = insecure; }

    public boolean isValid() {
        return url != null && !url.isBlank()
            && user != null && !user.isBlank()
            && password != null && !password.isBlank();
    }

    // Apply CLI flag overrides (Priority 1)
    public void applyOverrides(String urlFlag, String userFlag, String passFlag, Boolean insecureFlag) {
        if (urlFlag != null && !urlFlag.isBlank()) this.url = urlFlag;
        if (userFlag != null && !userFlag.isBlank()) this.user = userFlag;
        if (passFlag != null && !passFlag.isBlank()) this.password = passFlag;
        if (insecureFlag != null) this.insecure = insecureFlag;
    }
}
