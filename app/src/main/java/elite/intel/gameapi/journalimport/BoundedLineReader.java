package elite.intel.gameapi.journalimport;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Reads a journal's lines from a stream whose contents nobody vouches for.
 * <p>
 * {@code BufferedReader.readLine} holds a whole line in memory however long it is, and a file that only looks like a
 * journal - a disk image renamed, a gigabyte of zeros in a crafted archive - can be one line from end to end. Here a
 * line longer than {@link #MAX_LINE_BYTES} is dropped as it streams past, and reading carries on at the next one.
 * The longest real journal lines ({@code StoredModules} on a carrier, a big {@code Market}) are a few hundred
 * kilobytes.
 * <p>
 * Bytes are decoded only once a whole line is in, so a multi-byte character can never be split. A {@code \r} before
 * the newline and the BOM on the first line are taken off; blank lines are skipped. A last line with no newline -
 * a journal the game was still writing when it was copied - is returned like any other.
 */
final class BoundedLineReader implements Closeable {

    static final int MAX_LINE_BYTES = 8 * 1024 * 1024;

    private static final int BUFFER = 64 * 1024;

    private final InputStream in;
    private final byte[] buffer = new byte[BUFFER];
    private final ByteArrayOutputStream line = new ByteArrayOutputStream();
    private int position;
    private int limit;
    private boolean firstLine = true;
    private boolean overlong;
    private boolean ended;
    private long bytesRead;
    private int overlongLines;

    BoundedLineReader(InputStream in) {
        this.in = in;
    }

    /**
     * The next non-blank line, or null at the end of the stream.
     *
     * @throws IOException when the stream breaks off - a damaged archive entry. Lines already returned stand.
     */
    String readLine() throws IOException {
        while (true) {
            if (position == limit) {
                if (ended) return take();
                limit = in.read(buffer);
                position = 0;
                if (limit <= 0) {
                    limit = 0;
                    ended = true;
                    continue;
                }
                bytesRead += limit;
            }
            while (position < limit) {
                byte b = buffer[position++];
                if (b == '\n') {
                    String done = take();
                    if (done != null) return done;
                } else if (!overlong) {
                    line.write(b);
                    if (line.size() > MAX_LINE_BYTES) {
                        overlong = true;
                        line.reset();
                    }
                }
            }
        }
    }

    /**
     * Bytes taken from the stream so far, for progress.
     */
    long bytesRead() {
        return bytesRead;
    }

    /**
     * Lines dropped for being longer than any journal line.
     */
    int overlongLines() {
        return overlongLines;
    }

    /**
     * The line collected so far, or null when it was blank or dropped.
     */
    private String take() {
        if (overlong) {
            overlong = false;
            overlongLines++;
            line.reset();
            firstLine = false;
            return null;
        }
        if (line.size() == 0) return null;
        String text = line.toString(StandardCharsets.UTF_8);
        line.reset();
        if (text.endsWith("\r")) text = text.substring(0, text.length() - 1);
        if (firstLine) {
            firstLine = false;
            if (!text.isEmpty() && text.charAt(0) == '﻿') text = text.substring(1);
        }
        return text.isBlank() ? null : text;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
