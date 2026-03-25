package io.github.atunkodev.web;

import com.github.mvysny.vaadinboot.VaadinBoot;
import com.github.mvysny.vaadinboot.common.WebServer;
import io.github.atunkodev.core.recipe.RecipeDiscoveryService;
import io.github.reqstool.annotations.Requirements;
import java.io.IOException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "webui", description = "Launch browser-based recipe browser")
public class WebUiCommand implements Runnable {

    private final RecipeDiscoveryService discoveryService;

    @Option(names = "--port", description = "Port to listen on (default: 8080)", defaultValue = "8080")
    private int port = 8080;

    public WebUiCommand(RecipeDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    public int getPort() {
        return port;
    }

    @Override
    @Requirements({"atunko:WEB_0001"})
    public void run() {
        RecipeHolder.init(discoveryService.discoverAll());
        try {
            new VaadinBoot() {
                @Override
                protected void onStarted(WebServer server) throws IOException {
                    super.onStarted(server);
                    openBrowser("http://localhost:" + port);
                }
            }.withPort(port).run();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start Web UI: " + e.getMessage(), e);
        }
    }

    private static void openBrowser(String url) {
        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else if (os.contains("win")) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            } else {
                new ProcessBuilder("xdg-open", url).start();
            }
        } catch (IOException ignored) {
            // Browser open is best-effort; server is already running
        }
    }
}
