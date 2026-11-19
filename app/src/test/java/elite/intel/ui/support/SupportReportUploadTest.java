package elite.intel.ui.support;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The relay is a Worker outside this repository, so these tests pin the side of the contract the app owns: the form
 * it sends, and how each answer the relay can give is read. A local server stands in for the relay.
 */
class SupportReportUploadTest {

    private HttpServer relay;
    private final AtomicReference<String> received = new AtomicReference<>();
    private final AtomicReference<String> contentType = new AtomicReference<>();
    private volatile int answerStatus = 200;
    private volatile String answerBody = "{\"id\":\"K7QF3M\"}";

    @BeforeEach
    void startRelay() throws IOException {
        relay = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        relay.createContext("/report", exchange -> {
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            received.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] answer = answerBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(answerStatus, answer.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(answer);
            }
        });
        relay.start();
    }

    @AfterEach
    void stopRelay() {
        relay.stop(0);
    }

    private SupportReportUpload upload() {
        return new SupportReportUpload(URI.create("http://127.0.0.1:" + relay.getAddress().getPort() + "/report"));
    }

    private static Path bundle(Path dir) throws IOException {
        return Files.writeString(dir.resolve("bundle.zip"), "PK zip bytes", StandardCharsets.UTF_8);
    }

    @Test
    void sendsEveryFieldTheRelayReadsAndReturnsTheReportId(@TempDir Path tmp) throws Exception {
        SupportReportUpload.Report report = new SupportReportUpload.Report(
                "Crashed on jump\nsecond line", "cmdr@example.com", "linux", "1.2.0001", bundle(tmp));

        SupportReportUpload.Outcome outcome = upload().send(report);

        assertEquals(new SupportReportUpload.Outcome.Sent("K7QF3M"), outcome);
        assertTrue(contentType.get().startsWith("multipart/form-data; boundary="));
        String body = received.get();
        assertTrue(body.contains("name=\"description\"\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\nCrashed on jump\nsecond line\r\n"));
        assertTrue(body.contains("name=\"contact\"\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\ncmdr@example.com\r\n"));
        assertTrue(body.contains("name=\"os\"\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\nlinux\r\n"));
        assertTrue(body.contains("name=\"version\"\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\n1.2.0001\r\n"));
        assertTrue(body.contains("name=\"bundle\"; filename=\"support-bundle.zip\"\r\nContent-Type: application/zip\r\n\r\nPK zip bytes\r\n"));
    }

    @Test
    void leavesTheContactOutWhenTheCommanderGaveNoEmail(@TempDir Path tmp) throws Exception {
        upload().send(new SupportReportUpload.Report("broken", "  ", "windows", "1.2", bundle(tmp)));

        assertFalse(received.get().contains("name=\"contact\""));
    }

    @Test
    void readsTheRelaysRefusals() throws IOException {
        assertEquals(new SupportReportUpload.Outcome.TooLarge(), SupportReportUpload.outcome(413, "{\"error\":\"too_large\"}"));
        assertEquals(new SupportReportUpload.Outcome.RateLimited(), SupportReportUpload.outcome(429, "{\"error\":\"rate_limited\"}"));
        assertEquals(new SupportReportUpload.Outcome.Refused(400, "not_a_support_bundle"),
                SupportReportUpload.outcome(400, "{\"error\":\"not_a_support_bundle\"}"));
    }

    @Test
    void aRefusalThatIsNotTheRelaysJsonStillReportsItsStatus() throws IOException {
        assertEquals(new SupportReportUpload.Outcome.Refused(502, ""),
                SupportReportUpload.outcome(502, "<html>Bad gateway</html>"));
    }

    @Test
    void anAcceptanceWithoutAnIdIsAFailureNotASentReport() {
        assertThrows(IOException.class, () -> SupportReportUpload.outcome(200, "{}"));
    }

    @Test
    void aBundleOverTheRelaysLimitIsNotUploaded(@TempDir Path tmp) throws Exception {
        Path big = tmp.resolve("big.zip");
        Files.write(big, new byte[(int) SupportReportUpload.MAX_BUNDLE_BYTES + 1]);

        SupportReportUpload.Outcome outcome = upload().send(new SupportReportUpload.Report("broken", "", "linux", "1.2", big));

        assertEquals(new SupportReportUpload.Outcome.TooLarge(), outcome);
        assertNull(received.get());
    }

    @Test
    void aReportNeedsADescription(@TempDir Path tmp) throws IOException {
        Path bundle = bundle(tmp);
        assertThrows(IllegalArgumentException.class,
                () -> new SupportReportUpload.Report("   ", "", "linux", "1.2", bundle));
    }
}
