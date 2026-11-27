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
        assertFalse(page.getDescription().contains("materials ="), "an id that says its label is not repeated");
        page.validate();
    }

    @Test
    void tabsAreValuesAndOnlyRenamedOnesAreSpelledOut() {
        link.onMessage(text -> { }, """
                {"version":1,"type":"integration.register","client":"frameshift-command","pages":[
                 {"id":"commodities","label":"Commodities","tabs":[{"id":"my-cargo","label":"My Cargo"}]},
                 {"id":"navigation","label":"Route Planner"}]}""");

        ActionParameterSpec page = command.parameters().getFirst();
        assertEquals(List.of("commodities", "commodities/my-cargo", "navigation"), page.getEnumValues());
        assertTrue(page.getDescription().contains("navigation = Route Planner"));
        assertFalse(page.getDescription().contains("my-cargo ="));
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
