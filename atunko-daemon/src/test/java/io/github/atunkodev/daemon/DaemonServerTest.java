package io.github.atunkodev.daemon;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.core.engine.RecipeExecutionEngine;
import io.github.atunkodev.daemon.protocol.DaemonMessage;
import io.github.atunkodev.daemon.protocol.ProtocolCodec;
import io.github.reqstool.annotations.SVCs;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

class DaemonServerTest {

    private static final String RECIPE = "org.openrewrite.java.RemoveUnusedImports";
    private static final String VERSION = "0.1.0-TEST";

    @TempDir
    Path registryDir;

    @TempDir
    Path projectRoot;

    private DaemonServer server;
    private DaemonRegistry registry;
    private Thread serverThread;

    @BeforeEach
    void setUp() throws Exception {
        Files.createDirectories(projectRoot.resolve("src"));
        Files.writeString(projectRoot.resolve("src/A.java"), """
            import java.util.List;
            class A {}
            """);
        registry = new DaemonRegistry(registryDir, entry -> {});
    }

    private void start(Duration idleTimeout) throws Exception {
        server = new DaemonServer(projectRoot, VERSION, idleTimeout, new RecipeExecutionEngine(), registry);
        server.register();
        serverThread = new Thread(server::serve, "daemon-test");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.close();
        }
    }

    /** One request/response exchange, with the handshake. */
    private DaemonMessage exchange(String token, DaemonMessage request) throws Exception {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), server.port())) {
            Writer out = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
            BufferedReader in =
                    new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            ProtocolCodec.write(out, new DaemonMessage.Hello(token, VERSION));
            DaemonMessage handshake = ProtocolCodec.read(in);
            if (handshake instanceof DaemonMessage.Failure) {
                return handshake;
            }
            ProtocolCodec.write(out, request);
            return ProtocolCodec.read(in);
        }
    }

    /** Polls rather than sleeps a fixed time; no new test dependency is worth one await helper. */
    private static void awaitTrue(BooleanSupplier condition, String what) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Timed out waiting: " + what);
    }

    private DaemonMessage.Execute executeRecipe() {
        return new DaemonMessage.Execute(List.of(RECIPE), Map.of(), false);
    }

    @Test
    @Timeout(120)
    @SVCs({"atunko:SVC_CORE_0023"})
    void reusesParsedSourcesAcrossRequests() throws Exception {
        start(Duration.ofMinutes(5));

        DaemonMessage first = exchange(server.token(), executeRecipe());
        DaemonMessage second = exchange(server.token(), executeRecipe());

        assertThat(first).isInstanceOf(DaemonMessage.ExecuteResult.class);
        assertThat(((DaemonMessage.ExecuteResult) first).parsedFromCache())
                .as("first request must parse")
                .isFalse();
        assertThat(((DaemonMessage.ExecuteResult) second).parsedFromCache())
                .as("second request against an unchanged project must not re-parse")
                .isTrue();
    }

    @Test
    @Timeout(120)
    void reparsesAfterASourceFileChanges() throws Exception {
        start(Duration.ofMinutes(5));
        exchange(server.token(), executeRecipe());

        Files.writeString(projectRoot.resolve("src/A.java"), """
            import java.util.Map;
            class A { int x; }
            """);

        DaemonMessage after = exchange(server.token(), executeRecipe());

        assertThat(((DaemonMessage.ExecuteResult) after).parsedFromCache()).isFalse();
    }

    @Test
    @Timeout(60)
    @SVCs({"atunko:SVC_CORE_0023.3"})
    void refusesRequestWithWrongToken() throws Exception {
        start(Duration.ofMinutes(5));

        DaemonMessage response = exchange("not-the-token", executeRecipe());

        assertThat(response).isInstanceOf(DaemonMessage.Failure.class);
        assertThat(((DaemonMessage.Failure) response).message()).isEqualTo("unauthorized");
    }

    @Test
    @Timeout(60)
    void statusReportsProjectRootAndPid() throws Exception {
        start(Duration.ofMinutes(5));

        DaemonMessage response = exchange(server.token(), new DaemonMessage.Status());

        assertThat(response).isInstanceOf(DaemonMessage.StatusResult.class);
        DaemonMessage.StatusResult status = (DaemonMessage.StatusResult) response;
        assertThat(Path.of(status.projectRoot()))
                .isEqualTo(projectRoot.toAbsolutePath().normalize());
        assertThat(status.pid()).isEqualTo(ProcessHandle.current().pid());
        assertThat(status.atunkoVersion()).isEqualTo(VERSION);
    }

    @Test
    @Timeout(60)
    void stopRequestShutsTheDaemonDownAndDeregisters() throws Exception {
        start(Duration.ofMinutes(5));
        assertThat(registry.find(projectRoot)).isPresent();

        assertThat(exchange(server.token(), new DaemonMessage.Stop())).isInstanceOf(DaemonMessage.Ok.class);

        awaitTrue(() -> !serverThread.isAlive(), "daemon thread exits after a stop request");
        assertThat(registry.find(projectRoot)).isEmpty();
    }

    @Test
    @Timeout(60)
    @SVCs({"atunko:SVC_CORE_0023.2"})
    void exitsAfterIdleTimeout() throws Exception {
        start(Duration.ofMillis(500));

        awaitTrue(() -> !serverThread.isAlive(), "idle daemon exits on its own");
        assertThat(registry.find(projectRoot)).isEmpty();
    }

    @Test
    @Timeout(60)
    void registersItselfWithPortAndToken() throws Exception {
        start(Duration.ofMinutes(5));

        DaemonEntry entry = registry.find(projectRoot).orElseThrow();

        assertThat(entry.port()).isEqualTo(server.port());
        assertThat(entry.token()).isEqualTo(server.token());
        assertThat(entry.atunkoVersion()).isEqualTo(VERSION);
        assertThat(entry.isProcessAlive()).isTrue();
    }

    @Test
    void configuredIdleTimeoutAcceptsBothSecondsAndIso8601() {
        System.setProperty(DaemonServer.IDLE_TIMEOUT_PROPERTY, "90");
        try {
            assertThat(DaemonServer.configuredIdleTimeout()).isEqualTo(Duration.ofSeconds(90));
            System.setProperty(DaemonServer.IDLE_TIMEOUT_PROPERTY, "PT5M");
            assertThat(DaemonServer.configuredIdleTimeout()).isEqualTo(Duration.ofMinutes(5));
            System.setProperty(DaemonServer.IDLE_TIMEOUT_PROPERTY, "nonsense");
            assertThat(DaemonServer.configuredIdleTimeout()).isEqualTo(DaemonServer.DEFAULT_IDLE_TIMEOUT);
        } finally {
            System.clearProperty(DaemonServer.IDLE_TIMEOUT_PROPERTY);
        }
    }

    @Test
    @Timeout(60)
    void listensOnLoopbackOnly() throws Exception {
        start(Duration.ofMinutes(5));

        DaemonEntry entry = registry.find(projectRoot).orElseThrow();
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), entry.port())) {
            assertThat(socket.isConnected()).isTrue();
            assertThat(socket.getInetAddress().isLoopbackAddress()).isTrue();
        }
    }
}
