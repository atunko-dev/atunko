package io.github.atunkodev.core.git;

import io.github.reqstool.annotations.Requirements;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Orchestrates the pre-apply safety checkpoint: availability and repository checks, untracked-file detection, the
 * stash message format, and the classification callers need to report the result. Lives in core so the CLI, TUI and
 * Web UI share one policy instead of re-implementing it per frontend.
 */
public class GitCheckpointService {

    /** How a checkpoint attempt ended; every status other than {@link #FAILED} is an expected, benign outcome. */
    public enum Status {
        CREATED,
        /** Tracked files are unmodified — a stash would snapshot nothing. */
        NOTHING_TO_STASH,
        NO_GIT,
        NOT_A_REPOSITORY,
        FAILED
    }

    /**
     * @param checkpoint the created checkpoint, only for {@link Status#CREATED}
     * @param detail git's diagnostic, only for {@link Status#FAILED}
     * @param untrackedPresent untracked files exist, which a stash-based checkpoint cannot cover — set for both
     *     {@link Status#CREATED} and {@link Status#NOTHING_TO_STASH} so callers can warn honestly
     */
    public record Outcome(Status status, GitCheckpoint checkpoint, String detail, boolean untrackedPresent) {}

    private final GitService git;

    public GitCheckpointService() {
        this(new GitService());
    }

    public GitCheckpointService(GitService git) {
        this.git = git;
    }

    /** Attempts a stash-based checkpoint of {@code dir}; never throws and never touches the working tree. */
    @Requirements({"atunko:CORE_0006.3"})
    public Outcome checkpoint(Path dir) {
        if (!git.isGitAvailable()) {
            return new Outcome(Status.NO_GIT, null, null, false);
        }
        if (!git.isGitRepository(dir)) {
            return new Outcome(Status.NOT_A_REPOSITORY, null, null, false);
        }
        boolean untracked = git.hasUntrackedFiles(dir);
        String message = "atunko: pre-recipe " + Instant.now().truncatedTo(ChronoUnit.SECONDS);
        return switch (git.createCheckpoint(dir, message)) {
            case GitService.CheckpointCreation.Created created ->
                new Outcome(Status.CREATED, created.checkpoint(), null, untracked);
            case GitService.CheckpointCreation.NothingToStash ignored ->
                new Outcome(Status.NOTHING_TO_STASH, null, null, untracked);
            case GitService.CheckpointCreation.Failed failed ->
                new Outcome(Status.FAILED, null, failed.detail(), untracked);
        };
    }
}
