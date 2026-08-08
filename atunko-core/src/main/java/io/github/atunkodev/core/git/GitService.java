package io.github.atunkodev.core.git;

import io.github.reqstool.annotations.Requirements;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Git integration for the core engine — repository detection and stash-based safety checkpoints.
 *
 * <p>Delegates to the {@code git} command-line binary via {@link ProcessBuilder}; there is no JGit or other library
 * dependency. Every failure mode (missing binary, non-repository directory, git error) degrades gracefully to a
 * negative result — callers never see exceptions — but failures are distinguished from legitimate "nothing to do"
 * outcomes so callers can report them honestly.
 *
 * <p>Checkpoints are created with {@code git stash create} + {@code git stash store}, which records a stash entry
 * without ever touching the working tree, so the user's uncommitted changes cannot be lost. Note that unlike
 * {@code git stash push --include-untracked}, this snapshots tracked files only.
 */
public class GitService {

    private static final long GIT_TIMEOUT_SECONDS = 30;

    /** Outcome of {@link #createCheckpoint}: created, nothing stashable, or a git failure with its diagnostic. */
    public sealed interface CheckpointCreation {
        record Created(GitCheckpoint checkpoint) implements CheckpointCreation {}

        /** The tracked files are unmodified — there is nothing a stash could snapshot. */
        record NothingToStash() implements CheckpointCreation {}

        record Failed(String detail) implements CheckpointCreation {}
    }

    /** Returns {@code true} if the {@code git} binary is available on the PATH. */
    public boolean isGitAvailable() {
        return run(null, "git", "--version").succeeded();
    }

    /**
     * Returns {@code true} if {@code dir} is inside a git working tree.
     *
     * @param dir the directory to test
     */
    @Requirements({"atunko:CORE_0006.1"})
    public boolean isGitRepository(Path dir) {
        GitResult result = run(dir, "git", "rev-parse", "--is-inside-work-tree");
        return result.succeeded() && "true".equals(result.stdout().trim());
    }

    /**
     * Returns {@code true} when untracked (non-ignored) files exist in the working tree — files that a stash-based
     * checkpoint cannot cover.
     */
    public boolean hasUntrackedFiles(Path dir) {
        GitResult result = run(dir, "git", "ls-files", "--others", "--exclude-standard");
        return result.succeeded() && !result.stdout().isBlank();
    }

    /**
     * Records a stash-based safety checkpoint of the working tree without modifying it.
     *
     * <p>Uses {@code git stash create <message>} to build the stash commit and {@code git stash store} to register it
     * in {@code refs/stash}. The working tree is never touched, so no failure can lose uncommitted changes.
     *
     * @param dir a directory inside the git working tree to checkpoint
     * @param message the stash message, e.g. {@code "atunko: pre-recipe <timestamp>"}
     * @return {@link CheckpointCreation.Created} with the checkpoint, {@link CheckpointCreation.NothingToStash} when
     *     the tracked files are unmodified, or {@link CheckpointCreation.Failed} with git's diagnostic (for example a
     *     repository without an initial commit)
     */
    @Requirements({"atunko:CORE_0006.2"})
    public CheckpointCreation createCheckpoint(Path dir, String message) {
        GitResult create = run(dir, "git", "stash", "create", message);
        if (!create.succeeded()) {
            return new CheckpointCreation.Failed(create.diagnostic());
        }
        String sha = create.stdout().trim();
        if (sha.isEmpty()) {
            return new CheckpointCreation.NothingToStash();
        }
        GitResult store = run(dir, "git", "stash", "store", "-m", message, sha);
        if (!store.succeeded()) {
            return new CheckpointCreation.Failed(store.diagnostic());
        }
        return new CheckpointCreation.Created(new GitCheckpoint(sha, message));
    }

    private GitResult run(Path dir, String... command) {
        ProcessBuilder builder = new ProcessBuilder(command);
        if (dir != null) {
            builder.directory(dir.toFile());
        }
        try {
            Process process = builder.start();
            process.getOutputStream().close();
            // Drain both pipes on background threads: reading inline would block until the child closes its
            // streams and make the timeout unreachable for a hung git, while not draining could deadlock a
            // child that outgrows the pipe buffer before we wait.
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            Thread outReader = Thread.startVirtualThread(() -> drain(process.getInputStream(), stdout));
            Thread errReader = Thread.startVirtualThread(() -> drain(process.getErrorStream(), stderr));
            if (!process.waitFor(GIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return new GitResult(-1, "", "git timed out after " + GIT_TIMEOUT_SECONDS + " seconds");
            }
            // A grandchild inheriting the pipes (e.g. a spawned daemon) can keep them open past git's own
            // exit, so bound the drain instead of joining indefinitely; ByteArrayOutputStream is synchronized,
            // making the partial read safe.
            outReader.join(TimeUnit.SECONDS.toMillis(1));
            errReader.join(TimeUnit.SECONDS.toMillis(1));
            return new GitResult(
                    process.exitValue(),
                    stdout.toString(StandardCharsets.UTF_8),
                    stderr.toString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            return new GitResult(
                    -1,
                    "",
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new GitResult(-1, "", "interrupted while waiting for git");
        }
    }

    private static void drain(InputStream in, ByteArrayOutputStream out) {
        try {
            in.transferTo(out);
        } catch (IOException e) {
            // Pipe closed by destroyForcibly — whatever was drained so far is the diagnostic we have.
        }
    }

    private record GitResult(int exitCode, String stdout, String stderr) {
        boolean succeeded() {
            return exitCode == 0;
        }

        String diagnostic() {
            String trimmed = stderr.trim();
            return trimmed.isEmpty() ? "git exited with code " + exitCode : trimmed;
        }
    }
}
