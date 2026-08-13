package io.github.atunkodev.testing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Explains, in a test failure message, why a daemon did not serve a request.
 *
 * <p>A daemon problem is never an exception: the client falls back to in-process execution and, on the unreachable
 * path, drops the registry entry. A test that then looks the daemon up fails with a bare
 * {@code NoSuchElementException} that names no cause. The cause is in two places — the client's fallback reason and
 * the daemon's own log — so this pulls both into the message.
 *
 * <p>Into the message rather than into a CI artifact, because the registry directory is a JUnit {@code @TempDir}:
 * by the time any upload step ran, the logs would be gone.
 */
public final class DaemonDiagnostics {

    private static final int TAIL_LINES = 40;

    private DaemonDiagnostics() {}

    /**
     * @param registryDir the daemon registry directory, which is also where daemons write their logs
     * @param reason what the daemon attempt reported — a fallback reason, or captured command output
     */
    public static String describe(Path registryDir, String reason) {
        StringBuilder message = new StringBuilder("the daemon did not serve the request.\nReported: ")
                .append(reason)
                .append('\n');
        try (Stream<Path> entries = Files.list(registryDir)) {
            List<Path> logs = entries.filter(p -> p.getFileName().toString().endsWith(".log"))
                    .toList();
            if (logs.isEmpty()) {
                message.append("No daemon log in ").append(registryDir).append(" — the daemon never started.");
            }
            logs.forEach(log -> message.append(tail(log)));
        } catch (IOException e) {
            message.append("Could not list daemon logs in ")
                    .append(registryDir)
                    .append(": ")
                    .append(e);
        }
        return message.toString();
    }

    private static String tail(Path log) {
        StringBuilder text = new StringBuilder("\n--- last ")
                .append(TAIL_LINES)
                .append(" lines of ")
                .append(log)
                .append(" ---\n");
        try {
            List<String> lines = Files.readAllLines(log);
            lines.subList(Math.max(0, lines.size() - TAIL_LINES), lines.size())
                    .forEach(line -> text.append(line).append('\n'));
        } catch (IOException e) {
            text.append("(unreadable: ").append(e).append(")\n");
        }
        return text.toString();
    }
}
