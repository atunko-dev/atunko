package io.github.atunkodev.daemon.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.Writer;

/**
 * Reads and writes {@link DaemonMessage} frames as newline-delimited JSON.
 *
 * <p>One frame per line, so a reader can find the frame boundary without knowing the payload — the generator is
 * configured never to emit a raw newline inside a frame.
 */
public final class ProtocolCodec {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private ProtocolCodec() {}

    /** Writes one frame and flushes, so the peer is never left waiting on a buffered message. */
    public static void write(Writer out, DaemonMessage message) throws IOException {
        out.write(MAPPER.writeValueAsString(message));
        out.write('\n');
        out.flush();
    }

    /** Reads one frame. Throws {@link EOFException} when the peer closed the connection. */
    public static DaemonMessage read(BufferedReader in) throws IOException {
        String line = in.readLine();
        if (line == null) {
            throw new EOFException("connection closed before a frame arrived");
        }
        return MAPPER.readValue(line, DaemonMessage.class);
    }
}
