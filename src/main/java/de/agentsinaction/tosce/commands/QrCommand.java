package de.agentsinaction.tosce.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import de.agentsinaction.tosce.TosceCommand;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(
    name = "qr",
    description = "Generate QR code PDFs for login or examinees.",
    mixinStandardHelpOptions = true,
    subcommands = {
        QrCommand.LoginCommand.class,
        QrCommand.ExamineesCommand.class,
        HelpCommand.class
    }
)
public class QrCommand implements Callable<Integer> {

    @ParentCommand
    TosceCommand parent;

    @Override
    public Integer call() {
        new CommandLine(this).usage(System.out);
        return 0;
    }

    @Command(name = "login", description = "Download login QR codes as PDF. GET /rest/osces/{id}/login/qrcodes/pdf")
    static class LoginCommand implements Callable<Integer> {
        @ParentCommand QrCommand cmd;
        @Parameters(paramLabel = "OSCE_ID") long osceId;
        @Option(names = {"--lang"}, defaultValue = "de") String lang;
        @Option(names = {"--out", "-o"}, description = "Output PDF file")
        Path out;

        @Override
        public Integer call() throws Exception {
            cmd.parent.requireAuth();
            String path = "/rest/osces/" + osceId + "/login/qrcodes/pdf?lang=" + lang;
            byte[] data = cmd.parent.resolveClient().getBytes(path);
            Path target = out != null ? out : Path.of("qr-login-" + osceId + ".pdf");
            Files.write(target, data);
            ObjectNode result = cmd.parent.resolveClient().getMapper().createObjectNode();
            result.put("success", true);
            result.put("file", target.toAbsolutePath().toString());
            result.put("bytes", data.length);
            cmd.parent.printJson(result);
            return 0;
        }
    }

    @Command(name = "examinees", description = "Download examinee QR codes as ZIP. POST /rest/osces/examinees/qrcodes/images/zip")
    static class ExamineesCommand implements Callable<Integer> {
        @ParentCommand QrCommand cmd;
        @Parameters(paramLabel = "OSCE_ID") long osceId;
        @Option(names = {"--sorted"}, description = "Sort QR codes") boolean sorted;
        @Option(names = {"--out", "-o"}, description = "Output ZIP file")
        Path out;

        @Override
        public Integer call() throws Exception {
            cmd.parent.requireAuth();
            String path = "/rest/osces/examinees/qrcodes/images/zip?osceID=" + osceId + "&sorted=" + sorted;
            byte[] data = cmd.parent.resolveClient().getBytes(path);
            Path target = out != null ? out : Path.of("qr-examinees-" + osceId + ".zip");
            Files.write(target, data);
            ObjectNode result = cmd.parent.resolveClient().getMapper().createObjectNode();
            result.put("success", true);
            result.put("file", target.toAbsolutePath().toString());
            result.put("bytes", data.length);
            cmd.parent.printJson(result);
            return 0;
        }
    }
}
