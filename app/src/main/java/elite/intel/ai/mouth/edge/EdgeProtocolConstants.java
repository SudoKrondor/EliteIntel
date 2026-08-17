package elite.intel.ai.mouth.edge;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Constants mirrored from Microsoft Edge's consumer Read Aloud protocol and the upstream edge-tts client.
 * They are not Azure Speech credentials. Microsoft can change this private protocol, so the token, Chromium
 * version, endpoints, headers, or output format may require maintenance in a future release.
 */
final class EdgeProtocolConstants {
    static final String TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4";
    static final String CHROMIUM_FULL_VERSION = "143.0.3650.75";
    static final String SEC_MS_GEC_VERSION = "1-" + CHROMIUM_FULL_VERSION;
    static final String OUTPUT_FORMAT = "audio-24khz-48kbitrate-mono-mp3";
    static final int SAMPLE_RATE = 24_000;

    private static final String BASE_URL =
            "speech.platform.bing.com/consumer/speech/synthesize/readaloud";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0";

    private EdgeProtocolConstants() {
    }

    static URI voiceListUri(EdgeProtocolAuth auth) {
        return URI.create("https://" + BASE_URL + "/voices/list?trustedclienttoken="
                + TRUSTED_CLIENT_TOKEN + "&Sec-MS-GEC=" + auth.secMsGec()
                + "&Sec-MS-GEC-Version=" + SEC_MS_GEC_VERSION);
    }

    static URI synthesisUri(EdgeProtocolAuth auth, String connectionId) {
        return URI.create("wss://" + BASE_URL + "/edge/v1?TrustedClientToken="
                + TRUSTED_CLIENT_TOKEN + "&ConnectionId=" + connectionId
                + "&Sec-MS-GEC=" + auth.secMsGec()
                + "&Sec-MS-GEC-Version=" + SEC_MS_GEC_VERSION);
    }

    static Map<String, String> voiceHeaders(EdgeProtocolAuth auth) {
        Map<String, String> headers = baseHeaders();
        // WHY: Java HttpClient owns the HTTP authority pseudo-header; attempting to add Edge's "Authority"
        // spelling as a normal header is rejected. The URI produces the same wire authority.
        headers.put("Accept", "*/*");
        headers.put("Sec-CH-UA", "\" Not;A Brand\";v=\"99\", \"Microsoft Edge\";v=\"143\", "
                + "\"Chromium\";v=\"143\"");
        headers.put("Sec-CH-UA-Mobile", "?0");
        headers.put("Sec-Fetch-Site", "none");
        headers.put("Sec-Fetch-Mode", "cors");
        headers.put("Sec-Fetch-Dest", "empty");
        return auth.withMuid(headers);
    }

    static Map<String, String> webSocketHeaders(EdgeProtocolAuth auth) {
        Map<String, String> headers = baseHeaders();
        headers.put("Pragma", "no-cache");
        headers.put("Cache-Control", "no-cache");
        headers.put("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold");
        // WHY: Java's WebSocket implementation writes and validates Sec-WebSocket-Version itself.
        return auth.withMuid(headers);
    }

    private static Map<String, String> baseHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", USER_AGENT);
        headers.put("Accept-Language", "en-US,en;q=0.9");
        // WHY: upstream advertises gzip, Brotli, and Zstandard, but Java 21's BodyHandler does not decode those
        // encodings. Omitting Accept-Encoding requests an identity voice-list body with identical JSON content.
        return headers;
    }
}
