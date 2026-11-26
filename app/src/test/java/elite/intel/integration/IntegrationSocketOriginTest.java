package elite.intel.integration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A browser lets any web page open a socket to localhost; the Origin header is what keeps one out. */
class IntegrationSocketOriginTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "null", "file://", "app://frameshift", "http://localhost:5173",
            "https://127.0.0.1", "http://[::1]:3000", "HTTP://LOCALHOST"})
    void aDesktopProgramOrALocalPageConnects(String origin) {
        assertTrue(IntegrationSocket.isAllowedOrigin(origin));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com", "http://192.168.1.20", "http://localhost.evil.com",
            "https://evil.com/localhost", "not a uri"})
    void aWebPageFromElsewhereIsRefused(String origin) {
        assertFalse(IntegrationSocket.isAllowedOrigin(origin));
    }
}
