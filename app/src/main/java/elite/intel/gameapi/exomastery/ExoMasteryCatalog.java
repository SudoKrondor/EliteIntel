package elite.intel.gameapi.exomastery;

import com.google.gson.JsonParseException;
import elite.intel.util.json.GsonFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * The published Exo-Mastery catalogue: star systems whose planets carry high-value exobiology, as
 * reduced from a Spansh export and hosted at elite-intel.org.
 *
 * <p>This is the wire shape, one-to-one with the file: a system holds bodies, a body holds species.
 * The JSON carries the journal's own identifiers - {@code systemAddress} and {@code bodyId} - and the
 * Frontier species stem, so an entry joins to the location table and to a ScanOrganic event without
 * any name matching. The file is sorted best-first at every level, but nothing here relies on that:
 * the ranking that matters is done in SQL once the rows are in.
 *
 * <p>Gson fills the records reflectively, which is why the field names are the file's.
 */
public record ExoMasteryCatalog(int format, String generated, String source, int minColonies,
                                List<ExoSystem> systems) {

    /**
     * Where the catalogue lives. A plain static file, so a re-download costs nothing but bandwidth.
     */
    public static final String CATALOG_URL = "https://www.elite-intel.org/data/exomastery.json";

    /**
     * The one file layout this build can read. A newer layout is refused rather than half-read.
     */
    public static final int SUPPORTED_FORMAT = 1;

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(2);
    private static final int BUFFER = 64 * 1024;

    public record ExoSystem(String name, long systemAddress, double x, double y, double z, long value,
                            List<ExoBody> bodies) {
    }

    public record ExoBody(String name, long bodyId, String type, long value, List<ExoSpecies> species) {
    }

    public record ExoSpecies(String name, String symbol, int count, long value) {
    }

    /**
     * Downloads and parses the published catalogue.
     *
     * @param progress told the download percentage as bytes arrive, 0 to 100, on the calling thread. When the
     *                 server sends no length it hears only 100 at the end.
     * @throws IOException when the file cannot be fetched or is not a catalogue this build understands
     */
    public static ExoMasteryCatalog download(IntConsumer progress) throws IOException, InterruptedException {
        return download(URI.create(CATALOG_URL), progress);
    }

    static ExoMasteryCatalog download(URI uri, IntConsumer progress) throws IOException, InterruptedException {
        try (HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(CONNECT_TIMEOUT)
                .build()) {
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(REQUEST_TIMEOUT).GET().build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new IOException("Catalogue download failed: HTTP " + response.statusCode());
            }
            long length = response.headers().firstValueAsLong("Content-Length").orElse(-1);
            byte[] bytes;
            try (InputStream in = response.body()) {
                bytes = readAll(in, length, progress);
            }
            return parse(bytes);
        }
    }

    /**
     * Parses a catalogue file, refusing one whose layout this build does not know.
     */
    public static ExoMasteryCatalog parse(byte[] json) throws IOException {
        ExoMasteryCatalog catalog;
        try (InputStreamReader reader = new InputStreamReader(new java.io.ByteArrayInputStream(json), StandardCharsets.UTF_8)) {
            catalog = GsonFactory.getGson().fromJson(reader, ExoMasteryCatalog.class);
        } catch (JsonParseException e) {
            throw new IOException("Catalogue is not valid JSON: " + e.getMessage(), e);
        }
        if (catalog == null || catalog.systems() == null) {
            throw new IOException("Catalogue holds no systems");
        }
        if (catalog.format() != SUPPORTED_FORMAT) {
            throw new IOException("Catalogue format " + catalog.format() + " is newer than this build understands (" + SUPPORTED_FORMAT + ")");
        }
        return catalog;
    }

    private static byte[] readAll(InputStream in, long expected, IntConsumer progress) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(expected > 0 ? (int) Math.min(expected, Integer.MAX_VALUE) : BUFFER);
        byte[] buffer = new byte[BUFFER];
        long received = 0;
        int lastReported = -1;
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            received += read;
            if (expected > 0) {
                int percent = (int) Math.min(99, received * 100 / expected);
                if (percent != lastReported) {
                    progress.accept(percent);
                    lastReported = percent;
                }
            }
        }
        progress.accept(100);
        return out.toByteArray();
    }
}
