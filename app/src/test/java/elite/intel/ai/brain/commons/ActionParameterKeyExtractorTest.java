package elite.intel.ai.brain.commons;

import elite.intel.ai.brain.actions.command.CommandRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionParameterKeyExtractorTest {

    private final ActionParameterKeyExtractor extractor = ActionParameterKeyExtractor.getInstance();

    @BeforeAll
    static void loadRegistry() {
        CommandRegistry.getInstance().load();
    }

    @Test
    void extractsCoordinateKeysFromAliasTemplates() {
        assertEquals(List.of("lat", "lon"), extractor.parameterKeysForAction("navigate_to_coordinates"));
    }

    @Test
    void coordinateParamsTypedAsNumberFromRegistry() {
        // lat/lon типизируются как number из self-describing модели (CommandRegistry),
        // не из alias (alias даёт string) и не из удалённого JSON-примера.
        assertEquals(
                List.of(
                        new ActionParameterKeyExtractor.ActionParameterHint("lat", "number"),
                        new ActionParameterKeyExtractor.ActionParameterHint("lon", "number")
                ),
                extractor.parameterHintsForAction("navigate_to_coordinates")
        );
    }

    @Test
    void extractsKeysFromPromptJsonExamplesAndAliasTemplates() {
        assertTrue(extractor.parameterKeysForAction("target_subsystem").contains("key"));
        assertTrue(extractor.parameterKeysForAction("increase_speed").contains("key"));
        assertTrue(extractor.parameterKeysForAction("toggle_lights_on_off").contains("state"));
        assertTrue(extractor.parameterHintsForAction("toggle_lights_on_off")
                .contains(new ActionParameterKeyExtractor.ActionParameterHint("state", "boolean")));
    }

    @Test
    void returnsNoKeysForParameterlessCommands() {
        assertTrue(extractor.parameterKeysForAction("deploy_landing_gear").isEmpty());
    }
}
