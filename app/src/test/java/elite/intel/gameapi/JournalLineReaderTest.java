package elite.intel.gameapi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards {@link JournalLineReader}, the incremental file-read seam under
 * {@link JournalParser}.
 * <p>
 * The headline guard is {@link #readsLineLargerThanTheReadBuffer()}: a real
 * fleet-carrier {@code StoredModules} journal line exceeds 64&nbsp;KB. The
 * previous reader did a single fixed-buffer read per poll and discarded any
 * chunk containing no {@code \n}; once the file position reached such a line it
 * never advanced and every event after it was silently lost. This file pins the
 * fix and the surrounding incremental-read / encoding behavior.
 */
class JournalLineReaderTest {

    @TempDir
    Path tmp;

    private Path write(String name, byte[] bytes) throws IOException {
        Path p = tmp.resolve(name);
        Files.write(p, bytes);
        return p;
    }

    private Path write(String name, String content) throws IOException {
        return write(name, content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Build a single JSON line of at least {@code minBytes} (a fat StoredModules).
     */
    private static String hugeStoredModulesLine(int minBytes) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"timestamp\":\"2026-06-28T21:19:40Z\", \"event\":\"StoredModules\", \"Items\":[");
        int i = 0;
        while (sb.length() < minBytes) {
            if (i > 0) sb.append(',');
            sb.append("{\"Name\":\"$int_module_grade_a;\",\"StorageSlot\":").append(i)
                    .append(",\"StarSystem\":\"Shinrarta Dezhra\",\"MarketID\":128666762,\"Value\":12345}");
            i++;
        }
        sb.append("] }");
        return sb.toString();
    }

    @Test
    void readsLineLargerThanTheReadBuffer() throws IOException {
        String before = "{ \"event\":\"LoadGame\", \"part\":1 }";
        String huge = hugeStoredModulesLine(80_000);
        String after1 = "{ \"event\":\"Location\", \"StarSystem\":\"Sol\" }";
        String after2 = "{ \"event\":\"Shutdown\" }";

        assertTrue(huge.getBytes(StandardCharsets.UTF_8).length > 65536,
                "test fixture must exceed the reader's 64KB buffer");

        // CRLF, like a real Windows journal, and a final terminating newline.
        String content = before + "\r\n" + huge + "\r\n" + after1 + "\r\n" + after2 + "\r\n";
        Path file = write("Journal.big.log", content);

        JournalLineReader reader = new JournalLineReader(file);
        List<String> lines = reader.readNewLines();

        assertEquals(4, lines.size(), "every line, including those after the >64KB line, must be returned");
        assertEquals(before, lines.get(0));
        assertEquals(huge, lines.get(1));
        assertEquals(after1, lines.get(2), "the line right after the huge one must not be lost");
        assertEquals(after2, lines.get(3));
        assertEquals(content.getBytes(StandardCharsets.UTF_8).length, reader.getPosition(),
                "position must reach end of file");
    }

    @Test
    void leavesUnterminatedTrailingLineForNextRead() throws IOException {
        // Simulates the game flushing a line in two parts across two polls.
        String complete = "{ \"event\":\"Music\", \"MusicTrack\":\"Exploration\" }";
        String partial = "{ \"event\":\"FSDJump\", \"StarSyst";
        Path file = write("Journal.partial.log", complete + "\n" + partial);

        JournalLineReader reader = new JournalLineReader(file);
        List<String> first = reader.readNewLines();

        assertEquals(1, first.size(), "only the terminated line is returned");
        assertEquals(complete, first.get(0));
        long afterFirst = reader.getPosition();
        assertEquals((complete + "\n").getBytes(StandardCharsets.UTF_8).length, afterFirst,
                "position must sit at the start of the unterminated line");

        // Game flushes the rest of the line plus its newline and one more line.
        String rest = "em\":\"Sol\" }\n{ \"event\":\"Shutdown\" }\n";
        Files.write(file, rest.getBytes(StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.APPEND);

        List<String> second = reader.readNewLines();
        assertEquals(2, second.size());
        assertEquals("{ \"event\":\"FSDJump\", \"StarSystem\":\"Sol\" }", second.get(0),
                "the line split across two reads must be reassembled");
        assertEquals("{ \"event\":\"Shutdown\" }", second.get(1));
    }

    @Test
    void stripsCrlfAndSkipsBlankLines() throws IOException {
        String content = "{ \"event\":\"A\" }\r\n\r\n{ \"event\":\"B\" }\r\n";
        Path file = write("Journal.crlf.log", content);

        List<String> lines = new JournalLineReader(file).readNewLines();

        assertEquals(2, lines.size(), "blank line between events must be skipped");
        assertEquals("{ \"event\":\"A\" }", lines.get(0));
        assertEquals("{ \"event\":\"B\" }", lines.get(1));
        assertFalse(lines.get(0).endsWith("\r"), "carriage return must be stripped");
    }

    @Test
    void stripsUtf8BomFromFirstLineOnly() throws IOException {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        String text = "{ \"event\":\"Fileheader\" }\n{ \"event\":\"﻿LoadGame\" }\n";
        byte[] withBom = new byte[bom.length + text.getBytes(StandardCharsets.UTF_8).length];
        System.arraycopy(bom, 0, withBom, 0, bom.length);
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(textBytes, 0, withBom, bom.length, textBytes.length);
        Path file = write("Journal.bom.log", withBom);

        List<String> lines = new JournalLineReader(file).readNewLines();

        assertEquals(2, lines.size());
        assertEquals("{ \"event\":\"Fileheader\" }", lines.get(0),
                "leading BOM must be stripped from the first line");
        assertTrue(lines.get(1).contains("﻿"),
                "a BOM-like char inside a later line must be left untouched");
    }

    @Test
    void resumingMidFileDoesNotStripBom() throws IOException {
        // When constructed with a non-zero start position the first byte is not
        // the file's BOM, so nothing should be stripped.
        String content = "{ \"event\":\"A\" }\n﻿{ \"event\":\"B\" }\n";
        Path file = write("Journal.resume.log", content);
        long offset = "{ \"event\":\"A\" }\n".getBytes(StandardCharsets.UTF_8).length;

        List<String> lines = new JournalLineReader(file, offset).readNewLines();

        assertEquals(1, lines.size());
        assertTrue(lines.get(0).startsWith("﻿"),
                "resuming mid-file must not treat a line-leading BOM char as the file BOM");
    }
}
