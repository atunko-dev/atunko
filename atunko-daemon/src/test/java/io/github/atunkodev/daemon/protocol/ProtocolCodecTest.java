package io.github.atunkodev.daemon.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ProtocolCodecTest {

    static Stream<DaemonMessage> allMessageTypes() {
        return Stream.of(
                new DaemonMessage.Hello("token-abc", "0.1.0-SNAPSHOT"),
                new DaemonMessage.Execute(
                        List.of("org.openrewrite.java.RemoveUnusedImports"),
                        Map.of("org.openrewrite.java.RemoveUnusedImports", Map.of("option", "value")),
                        true),
                new DaemonMessage.Status(),
                new DaemonMessage.Stop(),
                new DaemonMessage.Ok(),
                new DaemonMessage.Failure("boom", true),
                new DaemonMessage.ExecuteResult(
                        List.of(new DaemonMessage.ChangedFile("src/A.java", "class A {}", "class B {}", "recipe")),
                        List.of("a warning"),
                        true),
                new DaemonMessage.StatusResult("/tmp/project", "0.1.0-SNAPSHOT", 1234L, 42L));
    }

    @ParameterizedTest
    @MethodSource("allMessageTypes")
    void roundTripsEveryMessageType(DaemonMessage original) throws Exception {
        StringWriter out = new StringWriter();
        ProtocolCodec.write(out, original);

        DaemonMessage decoded = ProtocolCodec.read(new BufferedReader(new StringReader(out.toString())));

        assertThat(decoded).isEqualTo(original);
    }

    @Test
    void writesExactlyOneLinePerFrame() throws Exception {
        StringWriter out = new StringWriter();
        ProtocolCodec.write(out, new DaemonMessage.Ok());
        ProtocolCodec.write(out, new DaemonMessage.Stop());

        assertThat(out.toString().lines()).hasSize(2);
    }

    @Test
    void diffContainingNewlinesStaysOnOneLine() throws Exception {
        DaemonMessage.ExecuteResult result = new DaemonMessage.ExecuteResult(
                List.of(new DaemonMessage.ChangedFile("A.java", "line1\nline2", "line1\nline2\nline3", "r")),
                List.of(),
                false);

        StringWriter out = new StringWriter();
        ProtocolCodec.write(out, result);

        assertThat(out.toString().lines()).hasSize(1);
        assertThat(ProtocolCodec.read(new BufferedReader(new StringReader(out.toString()))))
                .isEqualTo(result);
    }

    @Test
    void readingAClosedStreamReportsEndOfFile() {
        BufferedReader empty = new BufferedReader(new StringReader(""));

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> ProtocolCodec.read(empty)))
                .isInstanceOf(java.io.EOFException.class);
    }
}
