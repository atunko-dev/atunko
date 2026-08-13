package io.github.atunkodev.daemon;

import io.github.reqstool.annotations.Requirements;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Starts a daemon in a detached JVM.
 *
 * <p>Reuses this process's own java binary and classpath, which is what makes the launcher work identically from
 * the shadow JAR, from {@code gradlew run} and from a test — there is no install location to look up and no
 * assumption that {@code atunko} is on {@code PATH}.
 *
 * <p>The child is fully detached: its streams go to a log file rather than to inherited pipes, because a daemon
 * outliving the CLI that started it must not keep the terminal's file descriptors open.
 */
public class DaemonLauncher {

    /**
     * Maximum heap for a spawned daemon, in {@code -Xmx} syntax (e.g. {@code 1g}). Unset leaves the child on JVM
     * ergonomics, which give it a quarter of physical memory — fine on a workstation, too much on a shared CI
     * runner where several daemons and the build itself compete for the same memory.
     */
    public static final String MAX_HEAP_PROPERTY = "atunko.daemon.max-heap";

    private final DaemonRegistry registry;
    private final Duration startupTimeout;

    public DaemonLauncher(DaemonRegistry registry) {
        this(registry, Duration.ofSeconds(60));
    }

    public DaemonLauncher(DaemonRegistry registry, Duration startupTimeout) {
        this.registry = registry;
        this.startupTimeout = startupTimeout;
    }

    /**
     * Starts a daemon for {@code projectRoot} and waits until it has registered.
     *
     * @return the new daemon's entry, or empty if it did not come up in time
     */
    public Optional<DaemonEntry> launch(Path projectRoot, String atunkoVersion) throws IOException {
        Path root = DaemonRegistry.resolve(projectRoot);
        // Make room before starting, so the pool bound holds even under repeated cold starts.
        registry.evictToFit(DaemonRegistry.maxDaemons(), java.util.Set.of(root));

        Process process = new ProcessBuilder(command(root, atunkoVersion))
                .redirectErrorStream(true)
                .redirectOutput(logFile(root).toFile())
                .start();

        return awaitRegistration(root, process);
    }

    /** Package-visible so the memory bound can be asserted without launching a JVM. */
    @Requirements({"atunko:CORE_0023.5"})
    List<String> command(Path root, String atunkoVersion) {
        List<String> command = new ArrayList<>();
        command.add(javaBinary());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        String maxHeap = System.getProperty(MAX_HEAP_PROPERTY);
        if (maxHeap != null && !maxHeap.isBlank()) {
            command.add("-Xmx" + maxHeap);
        }
        // Carry the client's daemon configuration into the child; it has no other way to see it.
        forwardProperty(command, DaemonServer.IDLE_TIMEOUT_PROPERTY);
        forwardProperty(command, DaemonRegistry.MAX_DAEMONS_PROPERTY);
        forwardProperty(command, DaemonDirs.REGISTRY_DIR_PROPERTY);
        forwardProperty(command, MAX_HEAP_PROPERTY);
        command.add(DaemonMain.class.getName());
        command.add(root.toString());
        command.add(atunkoVersion);
        return command;
    }

    private static void forwardProperty(List<String> command, String key) {
        String value = System.getProperty(key);
        if (value != null && !value.isBlank()) {
            command.add("-D" + key + "=" + value);
        }
    }

    private static String javaBinary() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    /** Daemon output goes here — a crashed daemon is otherwise invisible, since nothing inherits its streams. */
    public static Path logFile(Path root) throws IOException {
        Path dir = DaemonDirs.registryDir();
        Files.createDirectories(dir);
        return dir.resolve(DaemonRegistry.keyFor(root) + ".log");
    }

    private Optional<DaemonEntry> awaitRegistration(Path root, Process process) {
        long deadline = System.nanoTime() + startupTimeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (!process.isAlive() && process.exitValue() != 0) {
                // Died on startup — no point waiting for an entry that will never appear.
                return Optional.empty();
            }
            Optional<DaemonEntry> entry = registry.find(root);
            if (entry.isPresent()) {
                return entry;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /** Where a user should look when a daemon fails to start. */
    public static String logHint(Path root) {
        try {
            return logFile(root).toString();
        } catch (IOException e) {
            return new File(DaemonDirs.registryDir().toFile(), "<key>.log").toString();
        }
    }
}
