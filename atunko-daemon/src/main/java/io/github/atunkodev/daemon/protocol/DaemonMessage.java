package io.github.atunkodev.daemon.protocol;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import java.util.Map;

/**
 * A single frame of the daemon wire protocol. Frames are newline-delimited JSON, tagged with a {@code type} field so
 * one stream can carry every message kind.
 *
 * <p>Only plain data crosses the wire — never an OpenRewrite {@code SourceFile}. LSTs cannot be serialized (see
 * {@code docs/design/lst-caching-serialization-superseded.md}), which is the whole reason the daemon exists: the LSTs
 * stay in the daemon's heap and only the execution outcome is sent back.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = DaemonMessage.Hello.class, name = "hello"),
    @JsonSubTypes.Type(value = DaemonMessage.Execute.class, name = "execute"),
    @JsonSubTypes.Type(value = DaemonMessage.Status.class, name = "status"),
    @JsonSubTypes.Type(value = DaemonMessage.Stop.class, name = "stop"),
    @JsonSubTypes.Type(value = DaemonMessage.Ok.class, name = "ok"),
    @JsonSubTypes.Type(value = DaemonMessage.Failure.class, name = "failure"),
    @JsonSubTypes.Type(value = DaemonMessage.ExecuteResult.class, name = "executeResult"),
    @JsonSubTypes.Type(value = DaemonMessage.StatusResult.class, name = "statusResult"),
})
public sealed interface DaemonMessage {

    /**
     * Opening frame of every connection. The daemon refuses the connection when {@code token} does not match its
     * registry entry, and the client refuses the daemon when {@code atunkoVersion} is not its own — LSTs parsed by a
     * different OpenRewrite must never be served.
     */
    record Hello(String token, String atunkoVersion) implements DaemonMessage {}

    /** Request to run recipes against the daemon's project. */
    record Execute(List<String> recipeNames, Map<String, Map<String, String>> recipeOptions, boolean dryRun)
            implements DaemonMessage {}

    /** Request for the daemon's own liveness and idle information. */
    record Status() implements DaemonMessage {}

    /** Request for an orderly shutdown. */
    record Stop() implements DaemonMessage {}

    /** Generic acknowledgement, used for {@link Stop} and a successful {@link Hello}. */
    record Ok() implements DaemonMessage {}

    /** Failure response. {@code retryable} tells the client whether falling back in-process is worth reporting. */
    record Failure(String message, boolean retryable) implements DaemonMessage {}

    /**
     * Outcome of an {@link Execute}. Carries the resulting file contents rather than LSTs, since the point of the
     * daemon is that the trees never leave its heap.
     */
    record ExecuteResult(List<ChangedFile> changedFiles, List<String> warnings, boolean parsedFromCache)
            implements DaemonMessage {}

    /**
     * One file the recipe run would change. Both sides of the change cross the wire because the client applies it
     * through the same {@code ChangeApplier} the in-process path uses, which needs before as well as after.
     */
    record ChangedFile(String path, String before, String after, String recipeName) {}

    /** Response to {@link Status}. */
    record StatusResult(String projectRoot, String atunkoVersion, long idleMillis, long pid) implements DaemonMessage {}
}
