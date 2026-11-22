package elite.intel.bindforge.devicefiles;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Reading a {@code .buttonMap} back gives the labels the writer was given. */
class ButtonMapReaderTest {

    @Test
    void whatTheWriterWritesReadsBackInOrder() throws IOException {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("Joy_2", "PINKY");
        labels.put("Joy_1", "LV MAIN TRIGGER");
        labels.put("Joy_XAxis", "ROLL & <PITCH>");

        Map<String, String> read = ButtonMapReader.read(ButtonMapWriter.write(labels));

        assertEquals(labels, read);
        assertEquals(List.copyOf(labels.keySet()), List.copyOf(read.keySet()));
    }

    /** A hand-edited file may put a label on its own indented line; the game shows the label, not the spaces. */
    @Test
    void whitespaceAroundALabelIsNotPartOfIt() throws IOException {
        byte[] file = "<Root>\n  <Joy_1>\n    TRIGGER\n  </Joy_1>\n  <Joy_1>SECOND</Joy_1>\n</Root>"
                .getBytes(StandardCharsets.UTF_8);

        assertEquals(Map.of("Joy_1", "TRIGGER"), ButtonMapReader.read(file));
    }

    @Test
    void aFileThatIsNotXmlIsReported() {
        byte[] broken = "<Root><Joy_1>broken</Root>".getBytes(StandardCharsets.UTF_8);

        assertThrows(IOException.class, () -> ButtonMapReader.read(broken));
    }
}
