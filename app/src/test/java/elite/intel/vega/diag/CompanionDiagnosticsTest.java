package elite.intel.vega.diag;

import com.google.gson.JsonObject;
import elite.intel.vega.model.llm.LlmToolDefinition;
import elite.intel.vega.model.llm.LlmToolInvocation;
import elite.intel.vega.prompt.Fact;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the pure rendering helpers of {@link CompanionDiagnostics} - name/call/args formatting and the
 * elision boundaries. The publish path (one-line glue over {@code UiBus}) is not exercised here.
 */
class CompanionDiagnosticsTest {

    private static final int MAX_TEXT = 400; // mirrors CompanionDiagnostics.MAX_TEXT
    private static final int MAX_ARGS = 300; // mirrors CompanionDiagnostics.MAX_ARGS

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
        assertEquals("[]", CompanionDiagnostics.names(List.of()));
    }

    @Test
    void namesJoinsToolNames() {
        assertEquals("[find_action, set_reminder]",
                CompanionDiagnostics.names(List.of(tool("find_action"), tool("set_reminder"))));
    }

    @Test
    void callsEmptyGivesNone() {
        assertEquals("none", CompanionDiagnostics.calls(List.of()));
    }

    @Test
    void callsRendersNameThenArgs() {
        LlmToolInvocation speak = new LlmToolInvocation("id", "speak", argsOf("text", "hi"));
        assertEquals("speak{\"text\":\"hi\"}", CompanionDiagnostics.calls(List.of(speak)));
    }

    @Test
    void argsEmptyOrNullGivesBraces() {
        assertEquals("{}", CompanionDiagnostics.args(null));
        assertEquals("{}", CompanionDiagnostics.args(new JsonObject()));
    }

    @Test
    void argsRendersCompactJson() {
        assertEquals("{\"text\":\"hi\"}", CompanionDiagnostics.args(argsOf("text", "hi")));
    }

    @Test
    void argsElidesJsonPastLimit() {
        String longValue = "x".repeat(400);
        String rendered = CompanionDiagnostics.args(argsOf("k", longValue));
        assertEquals(MAX_ARGS, rendered.length());
        assertTrue(rendered.endsWith("…"));
    }

    @Test
    void factRendersProvenanceTaggedText() {
        assertEquals("[system] current system Sol",
                CompanionDiagnostics.fact(new Fact("current system Sol", "system")));
    }

    @Test
    void truncateNullGivesEmpty() {
        assertEquals("", CompanionDiagnostics.truncate(null));
    }

    @Test
    void truncateShortTextUnchanged() {
        assertEquals("all stop", CompanionDiagnostics.truncate("all stop"));
    }

    @Test
    void truncateFlattensNewlines() {
        assertEquals("line1 line2", CompanionDiagnostics.truncate("line1\nline2"));
    }

    @Test
    void truncateKeepsTextAtLimit() {
        String atLimit = "a".repeat(MAX_TEXT);
        assertEquals(atLimit, CompanionDiagnostics.truncate(atLimit));
    }

    @Test
    void truncateElidesTextPastLimit() {
        String rendered = CompanionDiagnostics.truncate("a".repeat(MAX_TEXT + 40));
        assertEquals(MAX_TEXT, rendered.length());
        assertTrue(rendered.endsWith("…"));
    }
}
