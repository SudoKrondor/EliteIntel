package elite.intel.gameapi.journalimport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * What a file is, decided from its first bytes and never from its name.
 * <p>
 * WHY the bytes: a backup made on one machine and renamed on another says nothing reliable in its extension - a
 * {@code .zip} that is really a 7-Zip archive, a {@code .tgz} saved as {@code .gz}, a tar with no extension at all.
 * Every one of these formats opens with a signature, so the signature decides.
 */
enum ArchiveFormat {
    ZIP, SEVEN_Z, TAR, GZIP, BZIP2, XZ,
    /**
     * Recognised only to say so: there is no free decoder for it.
     */
    RAR,
    /**
     * None of the above - a journal, or anything else.
     */
    NONE;

    /**
     * How many leading bytes {@link #sniff} looks at: enough for the tar signature at offset 257.
     */
    static final int SNIFF_BYTES = 512;

    private static final byte[] ZIP_LOCAL = {'P', 'K', 3, 4};
    private static final byte[] ZIP_EMPTY = {'P', 'K', 5, 6};
    private static final byte[] ZIP_SPANNED = {'P', 'K', 7, 8};
    private static final byte[] SEVEN_Z_MAGIC = {'7', 'z', (byte) 0xBC, (byte) 0xAF, 0x27, 0x1C};
    private static final byte[] GZIP_MAGIC = {0x1F, (byte) 0x8B};
    private static final byte[] BZIP2_MAGIC = {'B', 'Z', 'h'};
    private static final byte[] XZ_MAGIC = {(byte) 0xFD, '7', 'z', 'X', 'Z', 0};
    private static final byte[] RAR_MAGIC = {'R', 'a', 'r', '!', 0x1A, 0x07};
    private static final byte[] TAR_MAGIC = "ustar".getBytes(StandardCharsets.US_ASCII);
    private static final int TAR_MAGIC_OFFSET = 257;

    static ArchiveFormat of(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            byte[] head = in.readNBytes(SNIFF_BYTES);
            return sniff(head, head.length);
        }
    }

    static ArchiveFormat sniff(byte[] head, int length) {
        if (startsWith(head, length, ZIP_LOCAL) || startsWith(head, length, ZIP_EMPTY)
                || startsWith(head, length, ZIP_SPANNED)) return ZIP;
        if (startsWith(head, length, SEVEN_Z_MAGIC)) return SEVEN_Z;
        if (startsWith(head, length, GZIP_MAGIC)) return GZIP;
        if (startsWith(head, length, BZIP2_MAGIC)) return BZIP2;
        if (startsWith(head, length, XZ_MAGIC)) return XZ;
        if (startsWith(head, length, RAR_MAGIC)) return RAR;
        if (isTar(head, length)) return TAR;
        return NONE;
    }

    /**
     * A compressed stream, which may hold a tar or a single file.
     */
    boolean isCompression() {
        return this == GZIP || this == BZIP2 || this == XZ;
    }

    /**
     * WHY the POSIX signature only: the pre-POSIX v7 tar has none, and guessing it from a header checksum would
     * take any 512 bytes that happened to add up for an archive. Every tar written this century carries it.
     */
    private static boolean isTar(byte[] head, int length) {
        if (length < TAR_MAGIC_OFFSET + TAR_MAGIC.length) return false;
        for (int i = 0; i < TAR_MAGIC.length; i++) {
            if (head[TAR_MAGIC_OFFSET + i] != TAR_MAGIC[i]) return false;
        }
        return true;
    }

    private static boolean startsWith(byte[] head, int length, byte[] magic) {
        if (length < magic.length) return false;
        for (int i = 0; i < magic.length; i++) {
            if (head[i] != magic[i]) return false;
        }
        return true;
    }
}
