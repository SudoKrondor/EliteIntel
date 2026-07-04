package elite.intel.gameapi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Incremental, byte-accurate reader of newline-delimited journal files.
 * <p>
 * This is the read seam extracted out of {@link JournalParser} so the
 * file-positioning logic can be exercised in isolation. It returns only
 * <em>complete</em> lines (terminated by {@code \n}) and remembers the byte
 * offset just past the last complete line, so the next call resumes exactly
 * where this one stopped. An unterminated trailing line (the game has not yet
 * flushed its newline) is left in the file and re-read next time.
 * <p>
 * Design constraints that this class exists to satisfy:
 * <ul>
 *   <li><b>Lines larger than the read buffer.</b> A fleet-carrier
 *       {@code StoredModules} (or large {@code Market}/{@code Outfitting}/
 *       {@code NavRoute}) event can exceed 64&nbsp;KB on a single line. The
 *       previous implementation did one fixed-buffer read per poll and dropped
 *       any chunk that contained no {@code \n}; once the file position reached
 *       such a line it never advanced again and silently stalled, so no event
 *       after that line was ever read. Here bytes are accumulated across
 *       multiple buffer reads until the line terminator is found.</li>
 *   <li><b>Multi-byte UTF-8 across a buffer boundary.</b> Bytes are accumulated
 *       raw and only decoded once a whole line has been collected, so a buffer
 *       edge can never land mid-character and corrupt the decode (and therefore
 *       the byte accounting).</li>
 *   <li><b>Windows {@code \r\n}.</b> A trailing carriage return is stripped.</li>
 *   <li><b>UTF-8 BOM.</b> Stripped from the very first line of the file only.</li>
 * </ul>
 * Position is advanced only for complete lines, so partial reads are safe to
 * retry. This class is not thread-safe; a single {@code JournalParser} thread
 * owns an instance.
 */
public class JournalLineReader {

    private static final int READ_BUFFER_SIZE = 65536;
    private static final char UTF8_BOM = '﻿';

    private final Path file;
    private long position;
    private boolean atFileStart;

    /**
     * Reads {@code file} from the beginning.
     */
    public JournalLineReader(Path file) {
        this(file, 0L);
    }

    /**
     * Reads {@code file} starting at {@code startPosition} bytes. The BOM is
     * stripped only when {@code startPosition == 0} (i.e. the first line of the
     * file is about to be read).
     */
    public JournalLineReader(Path file, long startPosition) {
        this.file = file;
        this.position = startPosition;
        this.atFileStart = startPosition == 0;
    }

    /**
     * Byte offset just past the last complete line returned so far.
     */
    public long getPosition() {
        return position;
    }

    /**
     * Opens the file, reads every complete line available from the current
     * {@link #getPosition() position}, advances the position past them, and
     * returns them in order. Blank lines are skipped. An unterminated trailing
     * line is left for a subsequent call.
     */
    public List<String> readNewLines() throws IOException {
        try (SeekableByteChannel channel = Files.newByteChannel(file, StandardOpenOption.READ)) {
            return readNewLines(channel);
        }
    }

    /**
     * Same as {@link #readNewLines()} but reads from a caller-supplied channel.
     * Package-private so tests can drive it with an in-memory or pre-positioned
     * channel without touching the filesystem path.
     */
    List<String> readNewLines(SeekableByteChannel channel) throws IOException {
        List<String> lines = new ArrayList<>();
        // Do NOT gate on channel.size(): on Windows the OS caches the
        // directory-entry size and may not reflect bytes the game has written
        // for minutes. A direct read from position reaches the real data.
        channel.position(position);

        ByteArrayOutputStream pending = new ByteArrayOutputStream();
        ByteBuffer buf = ByteBuffer.allocate(READ_BUFFER_SIZE);
        int bytesRead;
        while ((bytesRead = channel.read(buf)) > 0) {
            buf.flip();
            while (buf.hasRemaining()) {
                byte b = buf.get();
                if (b == '\n') {
                    byte[] lineBytes = pending.toByteArray();
                    pending.reset();
                    // Advance past the line and its terminating \n. Only complete
                    // lines move the position, so a half-written tail is re-read.
                    position += lineBytes.length + 1L;
                    String line = decodeLine(lineBytes);
                    if (!line.isBlank()) {
                        lines.add(line);
                    }
                } else {
                    pending.write(b);
                }
            }
            buf.clear();
        }
        // Whatever remains in `pending` is an unterminated trailing line; leave
        // `position` before it so it is re-read once the game flushes its \n.
        return lines;
    }

    private String decodeLine(byte[] lineBytes) {
        String line = new String(lineBytes, StandardCharsets.UTF_8);
        // Strip \r if the game wrote \r\n.
        if (line.endsWith("\r")) {
            line = line.substring(0, line.length() - 1);
        }
        // Strip the UTF-8 BOM from the very first line of the file.
        if (atFileStart) {
            if (!line.isEmpty() && line.charAt(0) == UTF8_BOM) {
                line = line.substring(1);
            }
            atFileStart = false;
        }
        return line;
    }
}