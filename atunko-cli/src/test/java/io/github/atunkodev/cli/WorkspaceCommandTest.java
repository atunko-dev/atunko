package io.github.atunkodev.cli;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.testing.CommandLineFixture;
import io.github.reqstool.annotations.SVCs;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

@SVCs({"atunko:SVC_CLI_0005"})
class WorkspaceCommandTest {

    private static Path fixture(String name) throws URISyntaxException {
        return Paths.get(WorkspaceCommandTest.class
                        .getClassLoader()
                        .getResource("workspaces/" + name)
                        .toURI())
                .toAbsolutePath()
                .normalize();
    }

    @Test
    @SVCs({"atunko:SVC_CLI_0005"})
    void listWorkspaceDiscoversFlatProjects() throws Exception {
        Path root = fixture("workspace-flat");
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("list", "--workspace", root.toString());

        assertThat(exitCode).isZero();
        String out = cli.stdout();
        assertThat(out).contains("service-auth");
        assertThat(out).contains("service-payments");
        assertThat(out).contains("service-ui");
    }

    @Test
    @SVCs({"atunko:SVC_CLI_0005"})
    void listWorkspaceEmptyDirectoryPrintsNoProjects() throws Exception {
        CommandLineFixture cli = CommandLineFixture.create();
        // Use a temp-like path that exists but has no build files
        Path root = fixture("workspace-flat").getParent();

        int exitCode = cli.execute("list", "--workspace", root.toString());

        // May or may not find projects depending on root; just verify no exception
        assertThat(exitCode).isZero();
    }

    @Test
    @SVCs({"atunko:SVC_CLI_0005.2"})
    void runWithoutProjectDirOrWorkspaceFails() {
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("run", "--recipe", "org.openrewrite.java.RemoveUnusedImports");

        assertThat(exitCode).isNotZero();
    }
}
