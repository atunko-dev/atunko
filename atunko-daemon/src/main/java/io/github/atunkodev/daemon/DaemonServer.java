package io.github.atunkodev.daemon;

import io.github.atunkodev.core.engine.ExecutionResult;
import io.github.atunkodev.core.engine.FileChange;
import io.github.atunkodev.core.engine.RecipeExecutionEngine;
import io.github.atunkodev.daemon.protocol.DaemonMessage;
import io.github.atunkodev.daemon.protocol.ProtocolCodec;
import io.github.reqstool.annotations.Requirements;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * A long-lived server holding one project's parsed sources and executing recipes against them on behalf of
 * short-lived CLI clients. This is the whole point of the daemon: the LSTs never leave this heap, because they
 * cannot be serialized.
 *
 * <p>Single-threaded by design. Requests for one project are serialized, which matches how the cache wants to be
 * used (one parse, not a race) and keeps the daemon's memory bounded to one project's trees.
 *
 * <p>Binds loopback only and checks a per-daemon token on every connection. Loopback alone is not an authorization
 * boundary on a shared host — any local user can reach {@code 127.0.0.1} — so the token, stored in an owner-only
 * registry file, is what actually gates access.
 */
@Requirements({"atunko:CORE_0023"})
public class DaemonServer implements AutoCloseable {

    /** System property overriding the idle timeout, as ISO-8601 duration or plain seconds. */
    public static final String IDLE_TIMEOUT_PROPERTY = "atunko.daemon.idle-timeout";

    /** Long enough to survive a think-and-retry loop, short enough not to hold 200 MB all afternoon. */
    public static final Duration DEFAULT_IDLE_TIMEOUT = Duration.ofMinutes(30);

    private final Path projectRoot;
    private final String token;
    private final String atunkoVersion;
    private final Duration idleTimeout;
    private final Supplier<RecipeExecutionEngine> engineSupplier;
    private volatile RecipeExecutionEngine engine;
    private final JavaSourcesCache cache;
    private final DaemonRegistry registry;

    private final ServerSocket serverSocket;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private volatile long lastRequestMillis = System.currentTimeMillis();

    public DaemonServer(
            Path projectRoot,
            String atunkoVersion,
            Duration idleTimeout,
            Supplier<RecipeExecutionEngine> engineSupplier,
            DaemonRegistry registry)
            throws IOException {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
        this.atunkoVersion = atunkoVersion;
        this.idleTimeout = idleTimeout;
        this.engineSupplier = engineSupplier;
        this.registry = registry;
        this.cache = new JavaSourcesCache(this.projectRoot);
        this.token = newToken();
        // Port 0: the OS picks a free ephemeral port, which the registry entry then advertises. Fixing a port
        // would collide with a second daemon and with anything else on the machine.
        this.serverSocket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress());
        // accept() must wake up periodically, otherwise an idle daemon blocks forever and never times out.
        this.serverSocket.setSoTimeout((int) Math.min(Integer.MAX_VALUE, idleTimeout.toMillis()));
    }

    /**
     * Convenience for callers holding an already-built engine (tests, in-process use).
     */
    public DaemonServer(
            Path projectRoot,
            String atunkoVersion,
            Duration idleTimeout,
            RecipeExecutionEngine engine,
            DaemonRegistry registry)
            throws IOException {
        this(projectRoot, atunkoVersion, idleTimeout, () -> engine, registry);
    }

    /**
     * Built on first use, not at startup. Constructing the engine scans the classpath for recipes, which takes long
     * enough on a cold JVM that doing it before {@link #register()} made clients time out waiting for the daemon to
     * appear.
     */
    private RecipeExecutionEngine engine() {
        RecipeExecutionEngine local = engine;
        if (local == null) {
            synchronized (this) {
                local = engine;
                if (local == null) {
                    local = engineSupplier.get();
                    engine = local;
                }
            }
        }
        return local;
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    public String token() {
        return token;
    }

    public Path projectRoot() {
        return projectRoot;
    }

    /** The registry entry describing this running daemon. */
    public DaemonEntry entry() {
        return new DaemonEntry(
                projectRoot, port(), ProcessHandle.current().pid(), atunkoVersion, token, System.currentTimeMillis());
    }

    /** Publishes this daemon so clients can find it. */
    public void register() {
        registry.write(entry());
    }

    /**
     * Serves connections until the idle timeout elapses or a client asks the daemon to stop.
     *
     * <p>Always deregisters on the way out, so a client never finds an entry for a daemon that has gone.
     */
    @Requirements({"atunko:CORE_0023", "atunko:CORE_0023.2"})
    public void serve() {
        try {
            while (running.get()) {
                try (Socket socket = serverSocket.accept()) {
                    lastRequestMillis = System.currentTimeMillis();
                    handle(socket);
                } catch (SocketTimeoutException e) {
                    if (idleFor().compareTo(idleTimeout) >= 0) {
                        running.set(false);
                    }
                } catch (IOException e) {
                    // A client that died mid-request must not take the daemon down with it.
                    if (serverSocket.isClosed()) {
                        running.set(false);
                    }
                }
            }
        } finally {
            registry.remove(projectRoot);
            closeQuietly();
        }
    }

    public Duration idleFor() {
        return Duration.ofMillis(System.currentTimeMillis() - lastRequestMillis);
    }

    private void handle(Socket socket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        Writer out = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);

        DaemonMessage first = ProtocolCodec.read(in);
        if (!(first instanceof DaemonMessage.Hello hello) || !authorized(hello)) {
            ProtocolCodec.write(out, new DaemonMessage.Failure("unauthorized", false));
            return;
        }
        ProtocolCodec.write(out, new DaemonMessage.Ok());

        DaemonMessage request = ProtocolCodec.read(in);
        switch (request) {
            case DaemonMessage.Execute execute -> ProtocolCodec.write(out, execute(execute));
            case DaemonMessage.Status ignored ->
                ProtocolCodec.write(
                        out,
                        new DaemonMessage.StatusResult(
                                projectRoot.toString(),
                                atunkoVersion,
                                idleFor().toMillis(),
                                ProcessHandle.current().pid()));
            case DaemonMessage.Stop ignored -> {
                ProtocolCodec.write(out, new DaemonMessage.Ok());
                running.set(false);
            }
            default -> ProtocolCodec.write(out, new DaemonMessage.Failure("unexpected request", false));
        }
        lastRequestMillis = System.currentTimeMillis();
        registry.touch(projectRoot);
    }

    /**
     * Constant-time token comparison. A timing oracle here would let a local attacker recover the token byte by
     * byte, which is exactly what the token exists to prevent.
     */
    @Requirements({"atunko:CORE_0023.3"})
    private boolean authorized(DaemonMessage.Hello hello) {
        String presented = hello.token();
        if (presented == null) {
            return false;
        }
        return java.security.MessageDigest.isEqual(
                presented.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
    }

    private DaemonMessage execute(DaemonMessage.Execute request) {
        try {
            int parsesBefore = cache.parseCount();
            var sources = cache.get();
            boolean fromCache = cache.parseCount() == parsesBefore;

            List<DaemonMessage.ChangedFile> changed = new ArrayList<>();
            for (String recipeName : request.recipeNames()) {
                ExecutionResult result = engine().execute(recipeName, sources);
                for (FileChange change : result.changes()) {
                    changed.add(new DaemonMessage.ChangedFile(
                            change.path().toString(), change.before(), change.after(), recipeName));
                }
            }
            return new DaemonMessage.ExecuteResult(changed, List.of(), fromCache);
        } catch (RuntimeException e) {
            // Reported as retryable: the client can still get a correct answer by running in-process.
            return new DaemonMessage.Failure(String.valueOf(e.getMessage()), true);
        }
    }

    @Override
    public void close() {
        running.set(false);
        closeQuietly();
    }

    private void closeQuietly() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            // Shutting down anyway.
        }
    }

    /** Parses the configured idle timeout, tolerating both {@code PT5M} and a plain seconds count. */
    public static Duration configuredIdleTimeout() {
        String raw = System.getProperty(IDLE_TIMEOUT_PROPERTY);
        if (raw == null || raw.isBlank()) {
            return DEFAULT_IDLE_TIMEOUT;
        }
        try {
            return raw.startsWith("P") ? Duration.parse(raw) : Duration.ofSeconds(Long.parseLong(raw.trim()));
        } catch (RuntimeException e) {
            return DEFAULT_IDLE_TIMEOUT;
        }
    }
}
