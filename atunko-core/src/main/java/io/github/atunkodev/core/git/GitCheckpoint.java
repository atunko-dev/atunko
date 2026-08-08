package io.github.atunkodev.core.git;

/**
 * A stash-based safety checkpoint recorded before a recipe run.
 *
 * @param stashSha the SHA of the stash commit — stable, unlike a {@code stash@{n}} index
 * @param message the stash message the checkpoint was stored under
 */
public record GitCheckpoint(String stashSha, String message) {

    /** The git command that restores the checkpointed working-tree state. */
    public String restoreCommand() {
        return "git stash apply " + stashSha;
    }
}
