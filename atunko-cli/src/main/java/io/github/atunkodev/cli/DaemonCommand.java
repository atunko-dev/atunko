package io.github.atunkodev.cli;

import io.github.atunkodev.daemon.AtunkoVersion;
import io.github.atunkodev.daemon.DaemonClient;
import io.github.atunkodev.daemon.DaemonEntry;
import io.github.atunkodev.daemon.DaemonRegistry;
import io.github.reqstool.annotations.Requirements;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * Inspection and control for the daemons {@code atunko run} starts on its own.
 *
 * <p>Auto-start is only defensible if the user can see what it started and stop it, which is what these
 * subcommands are for.
 */
@Command(
        name = "daemon",
        description = "Inspect and control the atunko daemons that cache parsed sources between runs",
        mixinStandardHelpOptions = true,
        subcommands = {DaemonCommand.StatusCommand.class, DaemonCommand.StopCommand.class})
public class DaemonCommand implements Runnable {

    @Spec
    private CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().getSubcommands().get("status").execute();
    }

    @Command(name = "status", description = "List the running daemons", mixinStandardHelpOptions = true)
    public static class StatusCommand implements Runnable {

        @Spec
        private CommandSpec spec;

        private final DaemonRegistry registry;

        public StatusCommand() {
            this(new DaemonRegistry());
        }

        public StatusCommand(DaemonRegistry registry) {
            this.registry = registry;
        }

        @Override
        @Requirements({"atunko:CLI_0009.3"})
        public void run() {
            PrintWriter out = spec.commandLine().getOut();
            List<DaemonEntry> entries = registry.list();

            if (entries.isEmpty()) {
                out.println("No atunko daemons running.");
                out.flush();
                return;
            }
            out.printf("%-8s %-7s %-9s %-16s %s%n", "PID", "PORT", "IDLE", "VERSION", "PROJECT");
            for (DaemonEntry entry : entries) {
                out.printf(
                        "%-8d %-7d %-9s %-16s %s%n",
                        entry.pid(), entry.port(), humanIdle(entry), entry.atunkoVersion(), entry.projectRoot());
            }
            out.flush();
        }

        /** Idle time from the registry's last-used stamp — no need to wake a daemon just to print a table. */
        private static String humanIdle(DaemonEntry entry) {
            Duration idle = Duration.ofMillis(Math.max(0, System.currentTimeMillis() - entry.lastUsedEpochMillis()));
            if (idle.toHours() > 0) {
                return idle.toHours() + "h" + (idle.toMinutesPart()) + "m";
            }
            return idle.toMinutes() > 0 ? idle.toMinutes() + "m" : idle.toSeconds() + "s";
        }
    }

    @Command(name = "stop", description = "Stop a running daemon", mixinStandardHelpOptions = true)
    public static class StopCommand implements Runnable {

        @Option(names = "--all", description = "Stop every running daemon")
        private boolean all;

        @Option(
                names = "--project-dir",
                description = "Project directory whose daemon should be stopped (default: current directory)")
        private Path projectDir = Path.of(".");

        @Spec
        private CommandSpec spec;

        private final DaemonRegistry registry;
        private final DaemonClient client;

        public StopCommand() {
            this(new DaemonRegistry(), new DaemonClient(AtunkoVersion.current()));
        }

        public StopCommand(DaemonRegistry registry, DaemonClient client) {
            this.registry = registry;
            this.client = client;
        }

        @Override
        @Requirements({"atunko:CLI_0009.3"})
        public void run() {
            PrintWriter out = spec.commandLine().getOut();
            List<DaemonEntry> targets = targets();

            if (targets.isEmpty()) {
                out.println(all ? "No atunko daemons running." : "No atunko daemon running for " + projectDir);
                out.flush();
                return;
            }
            for (DaemonEntry entry : targets) {
                client.stop(entry);
                out.println("Stopped daemon " + entry.pid() + " for " + entry.projectRoot());
            }
            out.flush();
        }

        private List<DaemonEntry> targets() {
            if (all) {
                return registry.list();
            }
            Optional<DaemonEntry> entry = registry.find(projectDir);
            return entry.map(List::of).orElseGet(List::of);
        }
    }
}
