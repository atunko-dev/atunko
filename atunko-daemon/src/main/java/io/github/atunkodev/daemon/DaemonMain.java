package io.github.atunkodev.daemon;

import io.github.atunkodev.core.engine.RecipeExecutionEngine;
import java.nio.file.Path;

/**
 * Entry point of a daemon JVM. Started by {@link DaemonLauncher} in a detached process, never by a user directly.
 *
 * <p>Arguments: project root, atunko version. Everything else comes from system properties so the launcher can pass
 * the same {@code atunko.daemon.*} configuration the client saw.
 */
public final class DaemonMain {

    private DaemonMain() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: DaemonMain <projectRoot> <atunkoVersion>");
            System.exit(2);
            return;
        }
        Path projectRoot = Path.of(args[0]);
        String version = args[1];

        DaemonRegistry registry = new DaemonRegistry();
        try (DaemonServer server = new DaemonServer(
                projectRoot, version, DaemonServer.configuredIdleTimeout(), RecipeExecutionEngine::new, registry)) {
            // Register as early as possible: the socket is already bound by the constructor, and the engine is
            // built lazily on the first request. Doing recipe discovery first made clients time out waiting for
            // this entry to appear.
            server.register();
            server.serve();
        }
    }
}
