package io.github.atunkodev.web;

import com.github.mvysny.vaadinboot.VaadinBoot;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.github.atunkodev.core.AppServices;
import io.github.atunkodev.core.engine.ChangeApplier;
import io.github.atunkodev.core.engine.ExecutionResult;
import io.github.atunkodev.core.engine.FileChange;
import io.github.atunkodev.core.engine.RecipeExecutionEngine;
import io.github.atunkodev.core.project.ProjectInfo;
import io.github.atunkodev.core.project.ProjectSourceParser;
import io.github.atunkodev.core.project.SessionHolder;
import io.github.atunkodev.core.recipe.RecipeInfo;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for Playwright e2e tests. Starts VaadinBoot on a random port with stub services that
 * produce canned diff output so that browser-rendered features (diff2html, badges) can be verified.
 */
abstract class PlaywrightTestBase {

    static final RecipeInfo LEAF_RECIPE =
            new RecipeInfo("org.test.Alpha", "Alpha Recipe", "Test recipe", Set.of("java"));

    private static VaadinBoot boot;
    private static int port;

    static Playwright playwright;
    static Browser browser;
    BrowserContext context;
    Page page;

    @BeforeAll
    static void startServer() throws Exception {
        port = findFreePort();

        RecipeHolder.init(List.of(LEAF_RECIPE));
        SessionHolder.init(Path.of("."), new ProjectInfo(List.of(), List.of()));

        AppServices.init(stubEngine(), stubParser(), stubApplier());

        boot = new VaadinBoot();
        boot.withPort(port);
        boot.start();

        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @AfterAll
    static void stopServer() throws Exception {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
        if (boot != null) {
            boot.stop("e2e tests finished");
        }
    }

    @BeforeEach
    void createPage() {
        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void closePage() {
        if (context != null) {
            context.close();
        }
    }

    String baseUrl() {
        return "http://localhost:" + port;
    }

    void navigateTo(String path) {
        page.navigate(baseUrl() + path);
    }

    private static RecipeExecutionEngine stubEngine() {
        return new RecipeExecutionEngine() {
            @Override
            public ExecutionResult execute(String recipeName, List<org.openrewrite.SourceFile> sources) {
                return new ExecutionResult(List.of(new FileChange(
                        Path.of("src/main/java/Hello.java"),
                        "public class Hello {\n    // old\n}\n",
                        "public class Hello {\n    // new\n}\n")));
            }
        };
    }

    private static ProjectSourceParser stubParser() {
        return new ProjectSourceParser() {
            @Override
            public List<org.openrewrite.SourceFile> parse(ProjectInfo projectInfo) {
                return List.of();
            }
        };
    }

    private static ChangeApplier stubApplier() {
        return new ChangeApplier() {
            @Override
            public void apply(Path projectDir, List<FileChange> changes) {
                // no-op for e2e tests
            }
        };
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
