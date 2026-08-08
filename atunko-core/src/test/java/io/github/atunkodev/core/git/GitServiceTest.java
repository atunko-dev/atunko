package io.github.atunkodev.core.git;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.reqstool.annotations.SVCs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SVCs({"atunko:SVC_CORE_0006"})
class GitServiceTest {

    private final GitService gitService = new GitService();

    @TempDir
    Path tempDir;

    @BeforeEach
    void assumeGitAvailable() {
        Assumptions.assumeTrue(gitService.isGitAvailable(), "git binary not available - skipping git tests");
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

    private String gitOutput(Path dir, String... args) throws IOException, InterruptedException {
        String[] command = new String[args.length + 1];
        command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);
        Process process = new ProcessBuilder(command)
                .directory(dir.toFile())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(process.waitFor()).isZero();
        return output;
    }

    /** Creates a git repo in {@code tempDir} with a committed {@code Hello.java}. */
    private Path initRepoWithCommit() throws IOException, InterruptedException {
        git(tempDir, "init");
        git(tempDir, "config", "user.email", "test@atunko.dev");
        git(tempDir, "config", "user.name", "atunko test");
        git(tempDir, "config", "commit.gpgsign", "false");
        Files.writeString(tempDir.resolve("Hello.java"), "class Hello {}\n");
        git(tempDir, "add", ".");
        git(tempDir, "commit", "-m", "initial");
        return tempDir;
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0006.1"})
    void isGitRepositoryReturnsTrueInsideWorkingTree() throws Exception {
        Path repo = initRepoWithCommit();

        assertThat(gitService.isGitRepository(repo)).isTrue();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0006.1"})
    void isGitRepositoryReturnsFalseOutsideWorkingTree() {
        assertThat(gitService.isGitRepository(tempDir)).isFalse();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0006.2"})
    void createCheckpointKeepsWorkingTreeChangesAndIsRestorable() throws Exception {
        Path repo = initRepoWithCommit();
        Path file = repo.resolve("Hello.java");
        String modified = "class Hello { int x; }\n";
        Files.writeString(file, modified);

        Optional<GitCheckpoint> checkpoint = gitService.createCheckpoint(repo, "atunko: pre-recipe test");

        // The checkpoint exists and is listed as a stash entry
        assertThat(checkpoint).isPresent();
        assertThat(checkpoint.orElseThrow().stashSha()).isNotBlank();
        assertThat(gitOutput(repo, "stash", "list")).contains("atunko: pre-recipe test");

        // The working tree was never touched - the uncommitted change is still there
        assertThat(file).hasContent(modified);

        // Undo works: revert the file, then apply the checkpoint to get the change back
        git(repo, "checkout", "--", "Hello.java");
        assertThat(file).hasContent("class Hello {}\n");
        git(repo, "stash", "apply", checkpoint.orElseThrow().stashSha());
        assertThat(file).hasContent(modified);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0006.2"})
    void createCheckpointOnCleanWorkingTreeReturnsEmpty() throws Exception {
        Path repo = initRepoWithCommit();

        assertThat(gitService.createCheckpoint(repo, "atunko: pre-recipe test")).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0006.2"})
    void createCheckpointOutsideRepositoryReturnsEmpty() {
        assertThat(gitService.createCheckpoint(tempDir, "atunko: pre-recipe test"))
                .isEmpty();
    }
}
