package elite.intel.gameapi.journalimport;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds the backups the import tests read: entries are path inside the archive to content, in order.
 */
final class TestArchives {

    private TestArchives() {
    }

    static Map<String, byte[]> entries(Object... pathThenContent) {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        for (int i = 0; i < pathThenContent.length; i += 2) {
            Object content = pathThenContent[i + 1];
            entries.put((String) pathThenContent[i], content instanceof byte[] bytes ? bytes
                    : content.toString().getBytes(StandardCharsets.UTF_8));
        }
        return entries;
    }

    static byte[] zip(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    static byte[] tar(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        writeTar(bytes, entries);
        return bytes.toByteArray();
    }

    static byte[] tarGz(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GzipCompressorOutputStream gzip = new GzipCompressorOutputStream(bytes)) {
            writeTar(gzip, entries);
        }
        return bytes.toByteArray();
    }

    static byte[] gzip(byte[] content) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            gzip.write(content);
        }
        return bytes.toByteArray();
    }

    static void sevenZ(Path file, Map<String, byte[]> entries) throws IOException {
        try (SevenZOutputFile sevenZ = new SevenZOutputFile(file.toFile())) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                SevenZArchiveEntry archiveEntry = new SevenZArchiveEntry();
                archiveEntry.setName(entry.getKey());
                archiveEntry.setSize(entry.getValue().length);
                sevenZ.putArchiveEntry(archiveEntry);
                sevenZ.write(entry.getValue());
                sevenZ.closeArchiveEntry();
            }
        }
    }

    static Path write(Path file, byte[] content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, content);
        return file;
    }

    private static void writeTar(OutputStream out, Map<String, byte[]> entries) throws IOException {
        TarArchiveOutputStream tar = new TarArchiveOutputStream(out);
        tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            TarArchiveEntry archiveEntry = new TarArchiveEntry(entry.getKey());
            archiveEntry.setSize(entry.getValue().length);
            tar.putArchiveEntry(archiveEntry);
            tar.write(entry.getValue());
            tar.closeArchiveEntry();
        }
        tar.finish();
    }
}
