package elite.intel.ai.mouth.edge;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/** Generates the MUID cookie and time-bound Sec-MS-GEC value used by Edge Read Aloud. */
final class EdgeProtocolAuth {
    private static final long WINDOWS_EPOCH_SECONDS = 11_644_473_600L;
    private static final long FILETIME_TICKS_PER_SECOND = 10_000_000L;
    private static final long TOKEN_WINDOW_SECONDS = 300L;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final Clock clock;
    private final Supplier<byte[]> muidBytes;
    private final AtomicLong clockSkewMillis = new AtomicLong();

    EdgeProtocolAuth() {
        this(Clock.systemUTC(), EdgeProtocolAuth::randomMuidBytes);
    }

    EdgeProtocolAuth(Clock clock, Supplier<byte[]> muidBytes) {
        this.clock = clock;
        this.muidBytes = muidBytes;
    }

    private String newMuid() {
        byte[] bytes = muidBytes.get();
        if (bytes == null || bytes.length != 16) {
            throw new IllegalStateException("Edge MUID source must provide exactly 16 bytes");
        }
        return HexFormat.of().withUpperCase().formatHex(bytes);
    }

    String secMsGec() {
        long unixSeconds = Instant.now(clock).plusMillis(clockSkewMillis.get()).getEpochSecond();
        long roundedSeconds = unixSeconds - Math.floorMod(unixSeconds, TOKEN_WINDOW_SECONDS);
        long filetimeTicks = Math.multiplyExact(
                Math.addExact(roundedSeconds, WINDOWS_EPOCH_SECONDS), FILETIME_TICKS_PER_SECOND);
        String input = filetimeTicks + EdgeProtocolConstants.TRUSTED_CLIENT_TOKEN;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().withUpperCase().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    Map<String, String> withMuid(Map<String, String> headers) {
        Map<String, String> combined = new LinkedHashMap<>(headers);
        if (combined.containsKey("Cookie")) {
            throw new IllegalArgumentException("Edge headers already contain a Cookie");
        }
        combined.put("Cookie", "muid=" + newMuid() + ";");
        return combined;
    }

    void adjustToServerDate(String serverDate) throws EdgeProtocolException {
        if (serverDate == null || serverDate.isBlank()) {
            throw new EdgeProtocolException("Edge returned 403 without a server Date header");
        }
        try {
            Instant server = ZonedDateTime.parse(serverDate, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
            Instant adjustedClient = Instant.now(clock).plusMillis(clockSkewMillis.get());
            clockSkewMillis.addAndGet(server.toEpochMilli() - adjustedClient.toEpochMilli());
        } catch (DateTimeParseException e) {
            throw new EdgeProtocolException("Edge returned an invalid server Date header", e);
        }
    }

    private static byte[] randomMuidBytes() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }
}
