package io.github.atunkodev.core.git;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.reqstool.annotations.SVCs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

        GitService.CheckpointCreation creation = gitService.createCheckpoint(repo, "atunko: pre-recipe test");

        // The checkpoint exists and is listed as a stash entry
        assertThat(creation).isInstanceOf(GitService.CheckpointCreation.Created.class);
        GitCheckpoint checkpoint = ((GitService.CheckpointCreation.Created) creation).checkpoint();
        assertThat(checkpoint.stashSha()).isNotBlank();
        assertThat(gitOutput(repo, "stash", "list")).contains("atunko: pre-recipe test");

        // The working tree was never touched - the uncommitted change is still there
        assertThat(file).hasContent(modified);

        // The advertised restore command really restores the snapshot: simulate a recipe overwriting the
        // file, then run the checkpoint's restore to get the pre-run state back.
        Files.writeString(file, "class Hello { /* recipe output */ }\n");
        git(repo, "restore", "--source=" + checkpoint.stashSha(), "--", ".");
        assertThat(file).hasContent(modified);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0006.2"})
    void createCheckpointOnCleanWorkingTreeReturnsEmpty() throws Exception {
        Path repo = initRepoWithCommit();

        assertThat(gitService.createCheckpoint(repo, "atunko: pre-recipe test"))
                .isInstanceOf(GitService.CheckpointCreation.NothingToStash.class);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0006.2"})
    void createCheckpointOutsideRepositoryFails() {
        assertThat(gitService.createCheckpoint(tempDir, "atunko: pre-recipe test"))
                .isInstanceOf(GitService.CheckpointCreation.Failed.class);
    }

    /** An unborn HEAD passes the repository check but cannot stash — this must surface as a failure, not as clean. */
    @Test
    @SVCs({"atunko:SVC_CORE_0006.2"})
    void createCheckpointWithoutInitialCommitFailsWithDiagnostic() throws Exception {
        git(tempDir, "init");
        git(tempDir, "config", "user.email", "test@atunko.dev");
        git(tempDir, "config", "user.name", "atunko test");
        Files.writeString(tempDir.resolve("Hello.java"), "class Hello {}\n");
        git(tempDir, "add", ".");

        GitService.CheckpointCreation creation = gitService.createCheckpoint(tempDir, "atunko: pre-recipe test");

        assertThat(creation).isInstanceOf(GitService.CheckpointCreation.Failed.class);
        assertThat(((GitService.CheckpointCreation.Failed) creation).detail()).isNotBlank();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0006.2"})
    void hasUntrackedFilesDetectsFilesAStashCannotCover() throws Exception {
        Path repo = initRepoWithCommit();
        assertThat(gitService.hasUntrackedFiles(repo)).isFalse();

        Files.writeString(repo.resolve("Untracked.java"), "class Untracked {}\n");

        assertThat(gitService.hasUntrackedFiles(repo)).isTrue();
        // Untracked-only changes stash nothing - callers need hasUntrackedFiles to warn honestly.
        assertThat(gitService.createCheckpoint(repo, "atunko: pre-recipe test"))
                .isInstanceOf(GitService.CheckpointCreation.NothingToStash.class);
    }
}
