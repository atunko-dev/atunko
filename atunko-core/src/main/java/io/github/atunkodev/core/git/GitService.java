package io.github.atunkodev.core.git;

import io.github.reqstool.annotations.Requirements;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Git integration for the core engine — repository detection and stash-based safety checkpoints.
 *
 * <p>Delegates to the {@code git} command-line binary via {@link ProcessBuilder}; there is no JGit or other library
 * dependency. Every failure mode (missing binary, non-repository directory, git error) degrades gracefully to a
 * negative result — callers never see exceptions.
 *
 * <p>Checkpoints are created with {@code git stash create} + {@code git stash store}, which records a stash entry
 * without ever touching the working tree, so the user's uncommitted changes cannot be lost. Note that unlike
 * {@code git stash push --include-untracked}, this snapshots tracked files only.
 */
public class GitService {

    private static final long GIT_TIMEOUT_SECONDS = 30;

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
     * Records a stash-based safety checkpoint of the working tree without modifying it.
     *
     * <p>Uses {@code git stash create <message>} to build the stash commit and {@code git stash store} to register it
     * in {@code refs/stash}. The working tree is never touched, so no failure can lose uncommitted changes.
     *
     * @param dir a directory inside the git working tree to checkpoint
     * @param message the stash message, e.g. {@code "atunko: pre-recipe <timestamp>"}
     * @return the created checkpoint, or empty when there is nothing to checkpoint (clean working tree, no commits
     *     yet) or git is unusable
     */
    @Requirements({"atunko:CORE_0006.2"})
    public Optional<GitCheckpoint> createCheckpoint(Path dir, String message) {
        GitResult create = run(dir, "git", "stash", "create", message);
        String sha = create.stdout().trim();
        if (!create.succeeded() || sha.isEmpty()) {
            return Optional.empty();
        }
        GitResult store = run(dir, "git", "stash", "store", "-m", message, sha);
        if (!store.succeeded()) {
            return Optional.empty();
        }
        return Optional.of(new GitCheckpoint(sha, message));
    }

    private GitResult run(Path dir, String... command) {
        ProcessBuilder builder = new ProcessBuilder(command);
        if (dir != null) {
            builder.directory(dir.toFile());
        }
        builder.redirectError(ProcessBuilder.Redirect.DISCARD);
        try {
            Process process = builder.start();
            process.getOutputStream().close();
            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(GIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return new GitResult(-1, "");
            }
            return new GitResult(process.exitValue(), stdout);
        } catch (IOException e) {
            return new GitResult(-1, "");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new GitResult(-1, "");
        }
    }

    private record GitResult(int exitCode, String stdout) {
        boolean succeeded() {
            return exitCode == 0;
        }
    }
}
