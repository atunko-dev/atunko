package io.github.atunkodev.cli;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.testing.CommandLineFixture;
import io.github.reqstool.annotations.SVCs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RunCommandGitCheckpointTest {

    private static final Path FIXTURE_DIR =
            Path.of("../atunko-core/src/test/resources/fixtures/java-with-unused-imports");

    private static final String RECIPE = "org.openrewrite.java.RemoveUnusedImports";

    @TempDir
    Path tempDir;

    private Path copyFixtureToTemp() throws IOException {
        Path source = FIXTURE_DIR.toAbsolutePath().normalize();
        Files.copy(source.resolve("Example.java"), tempDir.resolve("Example.java"));
        return tempDir;
    }

    private void git(Path dir, String... args) throws IOException, InterruptedException {
        String[] command = new String[args.length + 1];
        command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);
        Process process = new ProcessBuilder(command)
                .directory(dir.toFile())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        assertThat(exitCode)
                .as("git %s failed: %s", String.join(" ", args), output)
                .isZero();
    }

    private boolean gitAvailable() {
        try {
            Process process = new ProcessBuilder("git", "--version").start();
            return process.waitFor() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0006.3"})
    void runWithGitCheckpointOutsideRepoPrintsMessageAndStillExecutes() throws IOException {
        Assumptions.assumeTrue(gitAvailable(), "git binary not available - skipping git tests");
        Path workDir = copyFixtureToTemp();
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("run", "-r", RECIPE, "--project-dir", workDir.toString(), "--git-checkpoint");

        assertThat(exitCode)
                .as("stderr: %s, stdout: %s", cli.stderr(), cli.stdout())
                .isZero();
        assertThat(cli.stdout()).contains("Not a git repository").contains("continuing without checkpoint");
        assertThat(cli.stdout()).contains("Changed:");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0006.3"})
    void runWithGitCheckpointInDirtyRepoPrintsStashShaAndRestoreCommand() throws Exception {
        Assumptions.assumeTrue(gitAvailable(), "git binary not available - skipping git tests");
        Path workDir = copyFixtureToTemp();
        git(workDir, "init");
        git(workDir, "config", "user.email", "test@atunko.dev");
        git(workDir, "config", "user.name", "atunko test");
        git(workDir, "config", "commit.gpgsign", "false");
        git(workDir, "add", ".");
        git(workDir, "commit", "-m", "initial");
        // Dirty the working tree so there is something to checkpoint
        Path example = workDir.resolve("Example.java");
        Files.writeString(example, "// dirty\n" + Files.readString(example));
        CommandLineFixture cli = CommandLineFixture.create();

        int exitCode = cli.execute("run", "-r", RECIPE, "--project-dir", workDir.toString(), "--git-checkpoint");

        assertThat(exitCode)
                .as("stderr: %s, stdout: %s", cli.stderr(), cli.stdout())
                .isZero();
        assertThat(cli.stdout()).contains("Git checkpoint created: ").contains("Restore with: git stash apply ");

        Process stashList = new ProcessBuilder("git", "stash", "list")
                .directory(workDir.toFile())
                .redirectErrorStream(true)
                .start();
        String stashes = new String(stashList.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(stashList.waitFor()).isZero();
        assertThat(stashes).contains("atunko: pre-recipe");
    }
}
