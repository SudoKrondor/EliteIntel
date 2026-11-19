package elite.intel.ui.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * Sends a bug report - the commander's description and a {@link SupportBundle} - to the report relay, which posts
 * both to the team's private Matrix room.
 * <p>
 * The relay holds the Matrix bot's token, so the app knows only the relay's address and nothing secret ships in
 * it. The relay is a Cloudflare Worker kept outside this repository ({@code elite-intel-report-relay}); its form
 * fields and error codes are the contract this class speaks.
 */
public final class SupportReportUpload {

    public static final URI RELAY = URI.create("https://elite-intel-report-relay.elite-intel.workers.dev/report");

    /**
     * The relay refuses a bigger bundle. Checked here first, so a commander on a slow line is not made to wait
     * for an upload that cannot succeed.
     */
    static final long MAX_BUNDLE_BYTES = 25L * 1024 * 1024;

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(3);

    /**
     * One report as the commander filed it.
     *
     * @param description what went wrong, never blank
     * @param email       where the team can answer, or blank for no answer
     * @param os          {@code windows}, {@code linux}, {@code macos} or {@code other}
     * @param version     the app version
     * @param bundle      the Support Bundle zip
     */
    public record Report(String description, String email, String os, String version, Path bundle) {
        public Report {
            Objects.requireNonNull(description, "description");
            if (description.isBlank()) throw new IllegalArgumentException("description is blank");
            email = email == null ? "" : email.strip();
            Objects.requireNonNull(os, "os");
            Objects.requireNonNull(version, "version");
            Objects.requireNonNull(bundle, "bundle");
        }
    }

    /**
     * How the relay answered. A network failure is not an outcome: {@link #send} throws it.
     */
    public sealed interface Outcome {
        /** Posted to the room, under the id the commander can quote. */
        record Sent(String reportId) implements Outcome {
        }

        /** The bundle is over the relay's limit. */
        record TooLarge() implements Outcome {
        }

        /** Too many reports from this address in the last minute. */
        record RateLimited() implements Outcome {
        }

        /** Any other refusal, with the relay's error code (blank when the answer carried none). */
        record Refused(int status, String error) implements Outcome {
        }
    }

    private final URI relay;
    private final HttpClient http;

    public SupportReportUpload() {
        this(RELAY);
    }

    SupportReportUpload(URI relay) {
        this.relay = Objects.requireNonNull(relay, "relay");
        this.http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    }

    /**
     * Uploads the report and waits for the relay's answer. Blocks for as long as the upload takes, so never call
     * it on the EDT.
     *
     * @throws IOException when the bundle cannot be read or the relay cannot be reached
     */
    public Outcome send(Report report) throws IOException, InterruptedException {
        if (Files.size(report.bundle()) > MAX_BUNDLE_BYTES) return new Outcome.TooLarge();
        String boundary = "EliteIntelReport" + UUID.randomUUID().toString().replace("-", "");
        HttpRequest request = HttpRequest.newBuilder(relay)
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipart(boundary, report)))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        return outcome(response.statusCode(), response.body());
    }

    static Outcome outcome(int status, String body) throws IOException {
        if (status == 200) {
            String id = field(body, "id");
            if (id.isBlank()) throw new IOException("the relay accepted the report but returned no report id");
            return new Outcome.Sent(id);
        }
        if (status == 413) return new Outcome.TooLarge();
        if (status == 429) return new Outcome.RateLimited();
        return new Outcome.Refused(status, field(body, "error"));
    }

    /**
     * A string field of the relay's JSON answer, or blank when the answer is not that JSON.
     */
    private static String field(String body, String name) {
        // WHY: a refusal can come from Cloudflare itself rather than the relay, as an HTML page. The status code
        // still says what happened, so a body that is not the relay's JSON costs only the error code's detail.
        try {
            JsonElement parsed = JsonParser.parseString(body == null ? "" : body);
            if (!parsed.isJsonObject()) return "";
            JsonObject object = parsed.getAsJsonObject();
            JsonElement value = object.get(name);
            return value != null && value.isJsonPrimitive() ? value.getAsString() : "";
        } catch (JsonSyntaxException notJson) {
            return "";
        }
    }

    private static byte[] multipart(String boundary, Report report) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        textPart(body, boundary, "description", report.description());
        if (!report.email().isEmpty()) textPart(body, boundary, "contact", report.email());
        textPart(body, boundary, "os", report.os());
        textPart(body, boundary, "version", report.version());
        write(body, "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"bundle\"; filename=\"support-bundle.zip\"\r\n"
                + "Content-Type: application/zip\r\n\r\n");
        body.write(Files.readAllBytes(report.bundle()));
        write(body, "\r\n--" + boundary + "--\r\n");
        return body.toByteArray();
    }

    private static void textPart(ByteArrayOutputStream body, String boundary, String name, String value) {
        write(body, "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n\r\n"
                + value + "\r\n");
    }

    private static void write(ByteArrayOutputStream body, String text) {
        body.writeBytes(text.getBytes(StandardCharsets.UTF_8));
    }
}
