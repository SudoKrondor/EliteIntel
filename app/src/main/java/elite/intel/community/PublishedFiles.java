package elite.intel.community;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * The two files the nightly job and the maintainers publish on the website, which this app reads once a day:
 * the checked massacre sets, and the switches that can turn uploads down without a release.
 * <p>
 * WHY plain files and not Firestore: reads are what Firestore bills, and every install reading every set would
 * cost thousands of times what one nightly read does. A static file costs nothing to fetch.
 */
final class PublishedFiles {

    static final URI SETS = URI.create("https://www.elite-intel.org/data/massacre-sets.json");
    static final URI CONFIG = URI.create("https://www.elite-intel.org/data/config.json");

    private static final int SETS_FORMAT = 1;
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    /**
     * The maintainers' switches. Either one off stops that upload on every install at its next check. Missing
     * means on.
     */
    record Switches(boolean heartbeat, boolean reportPairs) {
        static final Switches DEFAULT = new Switches(true, true);
    }

    /**
     * A download that came back unchanged ({@code sets} null) or with new content and its ETag.
     */
    record SetsDownload(List<CrowdSet> sets, Integer contributors, String etag) {
    }

    /**
     * What one published file holds: the sets this build can use, and how many installs stand behind them (null
     * when the file does not say).
     */
    record SetsFile(List<CrowdSet> sets, Integer contributors) {
    }

    private final URI setsUri;
    private final URI configUri;
    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(TIMEOUT)
            .build();

    PublishedFiles() {
        this(SETS, CONFIG);
    }

    /**
     * For tests, which point both at a local stub.
     */
    PublishedFiles(URI setsUri, URI configUri) {
        this.setsUri = setsUri;
        this.configUri = configUri;
    }

    /**
     * @param etag what the last download returned, so an unchanged file is not downloaded again, or null
     */
    SetsDownload downloadSets(String etag) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(setsUri).timeout(TIMEOUT).GET();
        if (etag != null) request.header("If-None-Match", etag);
        HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 304) return new SetsDownload(null, null, etag);
        if (response.statusCode() != 200) throw new IOException("Massacre sets download failed: HTTP " + response.statusCode());
        SetsFile file = parseSets(response.body());
        return new SetsDownload(file.sets(), file.contributors(), response.headers().firstValue("ETag").orElse(null));
    }

    /**
     * The switches, or {@link Switches#DEFAULT} when the file is not there. A file that is there but cannot be
     * read throws, so a broken switch file never turns uploads back on by accident.
     */
    Switches switches() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(configUri).timeout(TIMEOUT).GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) return Switches.DEFAULT;
        if (response.statusCode() != 200) throw new IOException("Config download failed: HTTP " + response.statusCode());
        try {
            JsonObject o = JsonParser.parseString(response.body()).getAsJsonObject();
            return new Switches(flag(o, "heartbeat"), flag(o, "reportPairs"));
        } catch (RuntimeException e) {
            throw new IOException("Config is not readable: " + e.getMessage(), e);
        }
    }

    /**
     * The sets in a published file. Only sets the nightly job could check against the galaxy index are kept: the
     * others have no coordinates, and this build has no way to check them yet.
     *
     * @throws IOException for a file this build does not understand
     */
    static SetsFile parseSets(String json) throws IOException {
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException("Massacre sets file is not valid JSON: " + e.getMessage(), e);
        }
        if (!root.has("format") || root.get("format").getAsInt() != SETS_FORMAT) {
            throw new IOException("Massacre sets format " + root.get("format") + " is not one this build reads");
        }
        List<CrowdSet> sets = new ArrayList<>();
        try {
            for (JsonElement e : root.getAsJsonArray("sets")) {
                JsonObject set = e.getAsJsonObject();
                if (!set.has("indexed") || !set.get("indexed").getAsBoolean()) continue;
                sets.add(crowdSet(set));
            }
        } catch (RuntimeException e) {
            // WHY: the file is ours, written by the nightly job, so a set missing a field is a bug there. The whole
            // download is refused and yesterday's sets stay in place, rather than half a file replacing them.
            throw new IOException("Massacre sets file does not have the expected shape: " + e.getMessage(), e);
        }
        Integer contributors = root.has("contributors") ? root.get("contributors").getAsInt() : null;
        return new SetsFile(sets, contributors);
    }

    private static CrowdSet crowdSet(JsonObject set) {
        JsonObject provider = set.getAsJsonObject("provider");
        JsonObject target = set.getAsJsonObject("target");
        JsonArray coords = provider.getAsJsonArray("coords");
        return new CrowdSet(
                provider.get("system").getAsString(),
                coords.get(0).getAsDouble(), coords.get(1).getAsDouble(), coords.get(2).getAsDouble(),
                strings(provider.getAsJsonArray("factions")),
                provider.has("stations") ? strings(provider.getAsJsonArray("stations")) : List.of(),
                target.get("system").getAsString(),
                target.getAsJsonArray("factions").get(0).getAsString(),
                set.get("stackDepth").getAsInt(),
                set.get("reporters").getAsInt(),
                set.has("lastSeen") ? set.get("lastSeen").getAsString() : null);
    }

    private static List<String> strings(JsonArray array) {
        List<String> values = new ArrayList<>();
        array.forEach(v -> values.add(v.getAsString()));
        return values;
    }

    private static boolean flag(JsonObject o, String name) {
        return !o.has(name) || o.get(name).getAsBoolean();
    }
}
