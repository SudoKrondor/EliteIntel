package elite.intel.gameapi.eddn;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DeflaterOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The listener's own work on one message off the wire, with no relay: inflating the zlib body and
 * handing the text on, and the two ways a message is dropped instead - too big to be a journal event,
 * or not zlib at all. Both drops must cost nothing but the message.
 */
class EddnListenerTest {

    private static final String ENVELOPE = """
            {"$schemaRef":"https://eddn.edcd.io/schemas/fsssignaldiscovered/1","message":{"StarSystem":"Ceos"}}""";

    @Test
    void aMessageIsInflatedAndHandedOnAsText() {
        List<String> handed = new ArrayList<>();
        EddnListener listener = new EddnListener(handed::add);

        listener.handle(deflate(ENVELOPE));

        assertEquals(List.of(ENVELOPE), handed);
        assertEquals(1, listener.handled(), "the handler said it learned from it");
    }

    @Test
    void aMessageTheHandlerDeclinesIsNotCountedAsUsed() {
        EddnListener listener = new EddnListener(envelope -> false);

        listener.handle(deflate(ENVELOPE));

        assertEquals(0, listener.handled());
    }

    @Test
    void aMessageThatIsNotZlibIsDroppedWithoutFailing() {
        List<String> handed = new ArrayList<>();
        EddnListener listener = new EddnListener(handed::add);

        assertDoesNotThrow(() -> listener.handle("not zlib at all".getBytes(StandardCharsets.UTF_8)),
                "one bad upload must not cost the connection");
        assertTrue(handed.isEmpty());
    }

    @Test
    void aMessageBiggerThanAnyJournalEventIsDroppedUnread() {
        List<String> handed = new ArrayList<>();
        EddnListener listener = new EddnListener(handed::add);
        // Five megabytes of one character deflate to a few kilobytes - exactly the shape of a
        // decompression bomb, and comfortably over the four the listener will inflate.
        String oversized = "x".repeat(5 * 1024 * 1024);

        listener.handle(deflate(oversized));

        assertTrue(handed.isEmpty(), "the cap is on the inflated size, not the wire size");
    }

    private static byte[] deflate(String text) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DeflaterOutputStream out = new DeflaterOutputStream(bytes)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return bytes.toByteArray();
    }
}
