package elite.intel.gameapi.journalimport;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lines out of a stream nobody vouches for: what a real journal holds comes out as written, and a "line" no journal
 * could hold is dropped without being held in memory.
 */
class BoundedLineReaderTest {

    @Test
    void journalLinesComeOutWithoutTheirBomCarriageReturnsOrBlankLines() throws IOException {
        List<String> lines = readAll("﻿{\"a\":1}\r\n\r\n{\"b\":2}\n   \n{\"c\":3}");
        assertEquals(List.of("{\"a\":1}", "{\"b\":2}", "{\"c\":3}"), lines,
                "the last line has no newline - a journal copied while the game was writing it - and still counts");
    }

    @Test
    void aCharacterSplitAcrossTwoReadsIsDecodedWhole() throws IOException {
        String line = "{\"StarSystem\":\"Ç" + "é".repeat(40_000) + "\"}";
        assertEquals(List.of(line), readAll(line + "\n"));
    }

    @Test
    void aLineLongerThanAnyJournalLineIsDroppedAndReadingGoesOn() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write("{\"before\":1}\n".getBytes(StandardCharsets.UTF_8));
        bytes.write(new byte[BoundedLineReader.MAX_LINE_BYTES + 10]);
        bytes.write("\n{\"after\":2}\n".getBytes(StandardCharsets.UTF_8));

        BoundedLineReader reader = new BoundedLineReader(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals("{\"before\":1}", reader.readLine());
        assertEquals("{\"after\":2}", reader.readLine());
        assertNull(reader.readLine());
        assertEquals(1, reader.overlongLines());
    }

    @Test
    void aStreamThatBreaksOffHasHandedOverEveryLineBeforeTheBreak() throws IOException {
        byte[] good = "{\"one\":1}\n{\"two\":2}\n".getBytes(StandardCharsets.UTF_8);
        InputStream breaking = new InputStream() {
            private int at;

            @Override
            public int read() throws IOException {
                if (at < good.length) return good[at++];
                throw new IOException("Unexpected end of ZLIB input stream");
            }
        };
        BoundedLineReader reader = new BoundedLineReader(breaking);
        assertEquals("{\"one\":1}", reader.readLine());
        assertEquals("{\"two\":2}", reader.readLine());
        assertThrows(IOException.class, reader::readLine);
    }

    private static List<String> readAll(String text) throws IOException {
        BoundedLineReader reader = new BoundedLineReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) lines.add(line);
        return lines;
    }
}
