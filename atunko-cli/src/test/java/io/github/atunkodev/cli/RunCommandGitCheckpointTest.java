package io.github.atunkodev.cli;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.core.engine.ChangeApplier;
import io.github.atunkodev.core.engine.RecipeExecutionEngine;
import io.github.atunkodev.core.git.GitCheckpointService;
import io.github.atunkodev.core.git.GitService;
import io.github.atunkodev.core.project.JavaSourceParser;
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
        return new GitService().isGitAvailable();
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
        assertThat(cli.stdout()).contains("Git checkpoint created: ").contains("Restore with: git restore --source=");

        Process stashList = new ProcessBuilder("git", "stash", "list")
                .directory(workDir.toFile())
                .redirectErrorStream(true)
                .start();
        String stashes = new String(stashList.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(stashList.waitFor()).isZero();
        assertThat(stashes).contains("atunko: pre-recipe");
    }

    /** The injected service lets the error branch be exercised without a real git failure. */
    @Test
    @SVCs({"atunko:SVC_CORE_0006.3"})
    void checkpointFailureIsReportedAsFailureAndRunContinues() throws IOException {
        Path workDir = copyFixtureToTemp();
        GitCheckpointService failing = new GitCheckpointService() {
            @Override
            public Outcome checkpoint(Path dir) {
                return new Outcome(Status.FAILED, null, "You do not have the initial commit yet", false);
            }
        };
        CommandLineFixture cli = CommandLineFixture.create(factoryWithCheckpointService(failing));

        int exitCode = cli.execute("run", "-r", RECIPE, "--project-dir", workDir.toString(), "--git-checkpoint");

        assertThat(exitCode)
                .as("stderr: %s, stdout: %s", cli.stderr(), cli.stdout())
                .isZero();
        assertThat(cli.stdout())
                .contains("Git checkpoint FAILED (You do not have the initial commit yet)")
                .doesNotContain("Working tree clean");
        assertThat(cli.stdout()).contains("Changed:");
    }

    /** Untracked-only changes stash nothing; the message must say so instead of claiming a clean tree. */
    @Test
    @SVCs({"atunko:SVC_CORE_0006.3"})
    void untrackedOnlyTreeWarnsInsteadOfClaimingClean() throws IOException {
        Path workDir = copyFixtureToTemp();
        GitCheckpointService untrackedOnly = new GitCheckpointService() {
            @Override
            public Outcome checkpoint(Path dir) {
                return new Outcome(Status.NOTHING_TO_STASH, null, null, true);
            }
        };
        CommandLineFixture cli = CommandLineFixture.create(factoryWithCheckpointService(untrackedOnly));

        int exitCode = cli.execute("run", "-r", RECIPE, "--project-dir", workDir.toString(), "--git-checkpoint");

        assertThat(exitCode).isZero();
        assertThat(cli.stdout()).contains("untracked files are NOT covered").doesNotContain("Working tree clean");
    }

    private static picocli.CommandLine.IFactory factoryWithCheckpointService(GitCheckpointService service) {
        return new picocli.CommandLine.IFactory() {
            private final picocli.CommandLine.IFactory defaults = picocli.CommandLine.defaultFactory();

            @Override
            public <K> K create(Class<K> cls) throws Exception {
                if (cls == RunCommand.class) {
                    return cls.cast(new RunCommand(
                            new RecipeExecutionEngine(), new JavaSourceParser(), new ChangeApplier(), service));
                }
                return defaults.create(cls);
            }
        };
    }
}
