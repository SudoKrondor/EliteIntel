package elite.intel.ai.brain.vega.diag;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.vega.model.llm.LlmToolDefinition;
import elite.intel.ai.brain.vega.model.llm.LlmToolInvocation;
import elite.intel.ai.brain.vega.prompt.Fact;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the pure rendering helpers of {@link VegaDiagnostics} - name/call/args formatting and the
 * elision boundaries. The publish path (one-line glue over {@code UiBus}) is not exercised here.
 */
class VegaDiagnosticsTest {

    private static final int MAX_TEXT = 400; // mirrors VegaDiagnostics.MAX_TEXT
    private static final int MAX_ARGS = 300; // mirrors VegaDiagnostics.MAX_ARGS

    private static LlmToolDefinition tool(String name) {
        return new LlmToolDefinition(name, "", "", List.of());
    }

    private static JsonObject argsOf(String key, String value) {
        JsonObject o = new JsonObject();
        o.addProperty(key, value);
        return o;
    }

    @Test
    void namesEmptyGivesEmptyBrackets() {
        assertEquals("[]", VegaDiagnostics.names(List.of()));
    }

    @Test
    void namesJoinsToolNames() {
        assertEquals("[find_action, set_reminder]",
                VegaDiagnostics.names(List.of(tool("find_action"), tool("set_reminder"))));
    }

    @Test
    void callsEmptyGivesNone() {
        assertEquals("none", VegaDiagnostics.calls(List.of()));
    }

    @Test
    void callsRendersNameThenArgs() {
        LlmToolInvocation speak = new LlmToolInvocation("id", "speak", argsOf("text", "hi"));
        assertEquals("speak{\"text\":\"hi\"}", VegaDiagnostics.calls(List.of(speak)));
    }

    @Test
    void argsEmptyOrNullGivesBraces() {
        assertEquals("{}", VegaDiagnostics.args(null));
        assertEquals("{}", VegaDiagnostics.args(new JsonObject()));
    }

    @Test
    void argsRendersCompactJson() {
        assertEquals("{\"text\":\"hi\"}", VegaDiagnostics.args(argsOf("text", "hi")));
    }

    @Test
    void argsElidesJsonPastLimit() {
        String longValue = "x".repeat(400);
        String rendered = VegaDiagnostics.args(argsOf("k", longValue));
        assertEquals(MAX_ARGS, rendered.length());
        assertTrue(rendered.endsWith("…"));
    }

    @Test
    void factRendersProvenanceTaggedText() {
        assertEquals("[system] current system Sol",
                VegaDiagnostics.fact(new Fact("current system Sol", "system")));
    }

    @Test
    void truncateNullGivesEmpty() {
        assertEquals("", VegaDiagnostics.truncate(null));
    }

    @Test
    void truncateShortTextUnchanged() {
        assertEquals("all stop", VegaDiagnostics.truncate("all stop"));
    }

    @Test
    void truncateFlattensNewlines() {
        assertEquals("line1 line2", VegaDiagnostics.truncate("line1\nline2"));
    }

    @Test
    void truncateKeepsTextAtLimit() {
        String atLimit = "a".repeat(MAX_TEXT);
        assertEquals(atLimit, VegaDiagnostics.truncate(atLimit));
    }

    @Test
    void truncateElidesTextPastLimit() {
        String rendered = VegaDiagnostics.truncate("a".repeat(MAX_TEXT + 40));
        assertEquals(MAX_TEXT, rendered.length());
        assertTrue(rendered.endsWith("…"));
    }
}
