package elite.intel.ai.brain.actions.handlers.commands.builtin;

import com.google.gson.JsonObject;
import elite.intel.ai.brain.actions.ActionParameterSpec;
import elite.intel.integration.FrameShiftLink;
import elite.intel.integration.FrameShiftLinkAccess;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FrameShiftOpenPageCommandTest {

    private final FrameShiftLink link = FrameShiftLinkAccess.newLink();
    private final FrameShiftOpenPageCommand command = new FrameShiftOpenPageCommand(link);

    @Test
    void hiddenFromTheModelUntilFrameShiftRegisters() {
        assertFalse(command.isVisibleForLLM(null));

        link.onMessage(text -> { }, """
                {"version":1,"type":"integration.register","client":"frameshift-command",
                 "pages":[{"id":"materials","label":"Materials"}]}""");

        assertTrue(command.isVisibleForLLM(null));
    }

    @Test
    void thePageParameterIsLimitedToTheRegisteredIds() {
        link.onMessage(text -> { }, """
                {"version":1,"type":"integration.register","client":"frameshift-command",
                 "pages":[{"id":"materials","label":"Materials"},{"id":"dashboard","label":"Dashboard"}]}""");

        List<ActionParameterSpec> parameters = command.parameters();
        assertEquals(1, parameters.size());
        ActionParameterSpec page = parameters.getFirst();
        assertEquals("page", page.getName());
        assertTrue(page.isRequired());
        assertEquals(List.of("materials", "dashboard"), page.getEnumValues());
        assertTrue(page.getDescription().contains("materials = Materials"));
        page.validate();
    }

    @Test
    void offlineIsSpokenRatherThanQueued() {
        JsonObject params = new JsonObject();
        params.addProperty("page", "materials");

        String spoken = command.execute(params, "");

        assertNotNull(spoken);
        assertFalse(spoken.isBlank());
    }
}
