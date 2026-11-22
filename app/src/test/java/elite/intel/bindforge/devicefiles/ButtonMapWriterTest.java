package elite.intel.bindforge.devicefiles;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ButtonMapWriterTest {

    @Test
    void labelsAreWrittenOnePerLineInTheMapsOrder() throws IOException {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("Joy_XAxis", "Left Toe Brake");
        labels.put("Joy_RZAxis", "Rudder");
        labels.put("Joy_1", "Trim & <Hold>");

        String written = new String(ButtonMapWriter.write(labels), StandardCharsets.UTF_8);

        assertEquals("""
                <?xml version="1.0" encoding="UTF-8"?>\r
                <Root>\r
                    <Joy_XAxis>Left Toe Brake</Joy_XAxis>\r
                    <Joy_RZAxis>Rudder</Joy_RZAxis>\r
                    <Joy_1>Trim &amp; &lt;Hold&gt;</Joy_1>\r
                </Root>\r
                """, written);
    }

    @Test
    void aTokenXmlCannotHoldIsRefused() {
        assertThrows(IOException.class, () -> ButtonMapWriter.write(Map.of("1 Button", "x")));
    }
}
