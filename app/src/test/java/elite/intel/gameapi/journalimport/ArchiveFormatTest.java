package elite.intel.gameapi.journalimport;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A backup is recognised by its first bytes. Its name is whatever the commander called it.
 */
class ArchiveFormatTest {

    @Test
    void eachFormatIsKnownByItsSignature() {
        assertEquals(ArchiveFormat.ZIP, sniff('P', 'K', 3, 4, 20));
        assertEquals(ArchiveFormat.ZIP, sniff('P', 'K', 5, 6), "an empty zip is still a zip");
        assertEquals(ArchiveFormat.SEVEN_Z, sniff('7', 'z', 0xBC, 0xAF, 0x27, 0x1C, 0));
        assertEquals(ArchiveFormat.GZIP, sniff(0x1F, 0x8B, 8));
        assertEquals(ArchiveFormat.BZIP2, sniff('B', 'Z', 'h', '9'));
        assertEquals(ArchiveFormat.XZ, sniff(0xFD, '7', 'z', 'X', 'Z', 0));
        assertEquals(ArchiveFormat.RAR, sniff('R', 'a', 'r', '!', 0x1A, 0x07, 0));
    }

    @Test
    void aTarIsKnownByTheSignatureInsideItsFirstHeader() {
        byte[] header = new byte[512];
        byte[] magic = "ustar".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(magic, 0, header, 257, magic.length);
        assertEquals(ArchiveFormat.TAR, ArchiveFormat.sniff(header, header.length));
    }

    @Test
    void aJournalIsNoArchive() {
        byte[] journal = "{ \"timestamp\":\"2024-01-01T00:00:00Z\", \"event\":\"Fileheader\" }"
                .getBytes(StandardCharsets.UTF_8);
        assertEquals(ArchiveFormat.NONE, ArchiveFormat.sniff(journal, journal.length));
        assertEquals(ArchiveFormat.NONE, ArchiveFormat.sniff(new byte[0], 0), "an empty file is nothing at all");
    }

    private static ArchiveFormat sniff(int... bytes) {
        byte[] head = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) head[i] = (byte) bytes[i];
        return ArchiveFormat.sniff(head, head.length);
    }
}
