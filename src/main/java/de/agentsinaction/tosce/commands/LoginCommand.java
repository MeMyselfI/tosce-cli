package de.agentsinaction.tosce.commands;

import com.fasterxml.jackson.databind.JsonNode;
import de.agentsinaction.tosce.TosceCommand;
import de.agentsinaction.tosce.client.ApiClient;
import de.agentsinaction.tosce.config.Config;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.util.concurrent.Callable;

@Command(
    name = "login",
    description = "Save server credentials to ~/.tosce/config and verify connection.\n" +
                  "Use global flags: tosce login --url <url> --user <user> --pass <password>"
)
public class LoginCommand implements Callable<Integer> {

    @ParentCommand
    TosceCommand parent;

    @Override
    public Integer call() {
        // Use the inherited --url/--user/--pass from the parent command
        String url = parent.url;
        String user = parent.user;
        String pass = parent.pass;
        Boolean insecureFlag = parent.insecure;

        if (url == null || url.isBlank()) {
            System.err.println("{\"success\":false,\"error\":\"--url is required for login\"}");
            return 1;
        }
        if (user == null || user.isBlank()) {
            System.err.println("{\"success\":false,\"error\":\"--user is required for login\"}");
            return 1;
        }
        if (pass == null || pass.isBlank()) {
            System.err.println("{\"success\":false,\"error\":\"--pass is required for login\"}");
            return 1;
        }

        Config config = Config.load();
        config.setUrl(url);
        config.setUser(user);
        config.setPassword(pass);
        config.setInsecure(Boolean.TRUE.equals(insecureFlag));

        ApiClient client = new ApiClient(config);
        try {
            client.get("/rest/conf");
            config.save();
            parent.printJson(client.getMapper()
                .createObjectNode()
                .put("success", true)
                .put("message", "Logged in successfully")
                .put("url", url)
                .put("user", user));
            return 0;
        } catch (Exception e) {
            System.err.println("{\"success\":false,\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
            return 1;
        }
    }
}
