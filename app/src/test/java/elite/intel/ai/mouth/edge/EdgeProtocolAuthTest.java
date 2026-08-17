package elite.intel.ai.mouth.edge;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EdgeProtocolAuthTest {
    private static final Instant NOW = Instant.parse("2024-01-02T03:04:05Z");

    @Test
    void generatesStableGecAndFreshMuidCookiePerRequest() {
        byte[] muid = new byte[16];
        for (int i = 0; i < muid.length; i++) {
            muid[i] = (byte) i;
        }
        AtomicInteger sequence = new AtomicInteger();
        EdgeProtocolAuth auth = new EdgeProtocolAuth(Clock.fixed(NOW, ZoneOffset.UTC), () -> {
            byte[] next = muid.clone();
            next[0] = (byte) sequence.getAndIncrement();
            return next;
        });

        assertEquals("BD721EDF522D70BE4575BAABDC730E8B6AE84F3A5FB57B5B31FE70FC89384262",
                auth.secMsGec());
        assertEquals("muid=000102030405060708090A0B0C0D0E0F;",
                auth.withMuid(Map.of("Accept", "*/*")).get("Cookie"));
        assertEquals("muid=010102030405060708090A0B0C0D0E0F;",
                auth.withMuid(Map.of("Accept", "*/*")).get("Cookie"));
    }

    @Test
    void serverDateAdjustsTheTokenWindowAndBadDatesFailClearly() throws Exception {
        EdgeProtocolAuth auth = new EdgeProtocolAuth(Clock.fixed(NOW, ZoneOffset.UTC), () -> new byte[16]);
        String before = auth.secMsGec();

        auth.adjustToServerDate("Tue, 2 Jan 2024 03:10:05 GMT");

        assertNotEquals(before, auth.secMsGec());
        assertThrows(EdgeProtocolException.class, () -> auth.adjustToServerDate(null));
        assertThrows(EdgeProtocolException.class, () -> auth.adjustToServerDate("not a date"));
    }

    @Test
    void rejectsInvalidMuidSourcesAndDuplicateCookies() {
        EdgeProtocolAuth invalid = new EdgeProtocolAuth(Clock.fixed(NOW, ZoneOffset.UTC), () -> new byte[15]);
        assertThrows(IllegalStateException.class, () -> invalid.withMuid(Map.of()));

        EdgeProtocolAuth valid = new EdgeProtocolAuth(Clock.fixed(NOW, ZoneOffset.UTC), () -> new byte[16]);
        assertThrows(IllegalArgumentException.class, () -> valid.withMuid(Map.of("Cookie", "existing")));
    }
}
