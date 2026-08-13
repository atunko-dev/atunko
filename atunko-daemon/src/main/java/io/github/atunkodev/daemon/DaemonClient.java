package io.github.atunkodev.daemon;

import io.github.atunkodev.daemon.protocol.DaemonMessage;
import io.github.atunkodev.daemon.protocol.ProtocolCodec;
import io.github.reqstool.annotations.Requirements;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Client side of the daemon: resolves a daemon for a project root, starting one if needed, and runs one request
 * against it.
 *
 * <p>Every failure mode here is recoverable by the caller running in-process instead, so nothing throws on the
 * unhappy path — {@link #execute} returns a result that says whether the daemon served it and, if not, why.
 */
public class DaemonClient {

    /** System property disabling the daemon entirely. */
    public static final String DISABLED_PROPERTY = "atunko.daemon.disabled";

    private final DaemonRegistry registry;
    private final DaemonLauncher launcher;
    private final String atunkoVersion;

    public DaemonClient(String atunkoVersion) {
        this(new DaemonRegistry(), atunkoVersion);
    }

    public DaemonClient(DaemonRegistry registry, String atunkoVersion) {
        this(registry, new DaemonLauncher(registry), atunkoVersion);
    }

    public DaemonClient(DaemonRegistry registry, DaemonLauncher launcher, String atunkoVersion) {
        this.registry = registry;
        this.launcher = launcher;
        this.atunkoVersion = atunkoVersion;
    }

    /** Whether the daemon is switched off by configuration. */
    @Requirements({"atunko:CLI_0009.2"})
    public static boolean disabledByConfiguration() {
        return Boolean.getBoolean(DISABLED_PROPERTY);
    }

    /**
     * Outcome of a daemon request.
     *
     * @param result the daemon's answer, or empty when the daemon could not serve it
     * @param fallbackReason why the daemon could not serve it — {@code null} when it did
     * @param startedDaemon whether this call had to start a daemon
     */
    public record Attempt(Optional<DaemonMessage.ExecuteResult> result, String fallbackReason, boolean startedDaemon) {

        static Attempt served(DaemonMessage.ExecuteResult result, boolean startedDaemon) {
            return new Attempt(Optional.of(result), null, startedDaemon);
        }

        static Attempt fallback(String reason, boolean startedDaemon) {
            return new Attempt(Optional.empty(), reason, startedDaemon);
        }
    }

    /**
     * Runs one execute request through a daemon for {@code projectRoot}.
     *
     * <p>Never throws for a daemon problem: an unreachable, crashed, unstartable or mismatched daemon all come back
     * as a fallback with a reason, because the caller can always still run the recipe in-process.
     */
    @Requirements({"atunko:CLI_0009", "atunko:CLI_0009.1"})
    public Attempt execute(Path projectRoot, DaemonMessage.Execute request) {
        Path root = DaemonRegistry.resolve(projectRoot);
        boolean started = false;

        Optional<DaemonEntry> existing = usableEntry(root);
        if (existing.isEmpty()) {
            try {
                existing = launcher.launch(root, atunkoVersion);
            } catch (IOException e) {
                return Attempt.fallback("could not start a daemon: " + e.getMessage(), false);
            }
            if (existing.isEmpty()) {
                // Deliberately does not name a cause: the launcher gives up both when the child exits early and
                // when it never registers, and claiming a timeout for a daemon that died on startup sends whoever
                // reads this looking in the wrong place. The log says which it was.
                return Attempt.fallback("daemon did not start (see " + DaemonLauncher.logHint(root) + ")", false);
            }
            started = true;
        }

        DaemonEntry entry = existing.get();
        try {
            DaemonMessage response = send(entry, request);
            if (response instanceof DaemonMessage.ExecuteResult result) {
                registry.touch(root);
                return Attempt.served(result, started);
            }
            String detail = response instanceof DaemonMessage.Failure failure ? failure.message() : "unexpected reply";
            return Attempt.fallback("daemon refused the request: " + detail, started);
        } catch (IOException e) {
            // The daemon died between the registry read and the request; drop the entry so the next run starts fresh.
            registry.remove(root);
            return Attempt.fallback("daemon became unreachable: " + e.getMessage(), started);
        }
    }

    /**
     * The registered daemon for this root, if it is one this client may use.
     *
     * <p>A daemon started by a different atunko version is stopped rather than reused: its cached LSTs were parsed
     * by that version's OpenRewrite, and serving them from a newer client would silently produce results neither
     * version would produce on its own.
     */
    @Requirements({"atunko:CORE_0023.4"})
    public Optional<DaemonEntry> usableEntry(Path projectRoot) {
        Path root = DaemonRegistry.resolve(projectRoot);
        Optional<DaemonEntry> entry = registry.find(root);
        if (entry.isPresent() && !atunkoVersion.equals(entry.get().atunkoVersion())) {
            stop(entry.get());
            return Optional.empty();
        }
        return entry;
    }

    /** Asks a daemon to shut down, falling back to killing it if it will not answer. */
    public void stop(DaemonEntry entry) {
        try {
            send(entry, new DaemonMessage.Stop());
        } catch (IOException e) {
            ProcessHandle.of(entry.pid()).ifPresent(ProcessHandle::destroy);
        }
        registry.remove(entry.projectRoot());
    }

    /** Queries a daemon for its status, or empty if it does not answer. */
    public Optional<DaemonMessage.StatusResult> status(DaemonEntry entry) {
        try {
            return send(entry, new DaemonMessage.Status()) instanceof DaemonMessage.StatusResult status
                    ? Optional.of(status)
                    : Optional.empty();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private DaemonMessage send(DaemonEntry entry, DaemonMessage request) throws IOException {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), entry.port())) {
            Writer out = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
            BufferedReader in =
                    new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            ProtocolCodec.write(out, new DaemonMessage.Hello(entry.token(), atunkoVersion));
            DaemonMessage handshake = ProtocolCodec.read(in);
            if (!(handshake instanceof DaemonMessage.Ok)) {
                return handshake;
            }
            ProtocolCodec.write(out, request);
            return ProtocolCodec.read(in);
        }
    }
}
