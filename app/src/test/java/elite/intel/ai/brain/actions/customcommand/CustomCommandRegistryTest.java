package elite.intel.ai.brain.actions.customcommand;

import com.google.gson.Gson;
import elite.intel.ai.brain.Reducer;
import elite.intel.ai.brain.actions.command.CommandHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CustomCommandRegistryTest {

    private static final Gson GSON = new Gson();
    private final CustomCommandRegistry registry = CustomCommandRegistry.getInstance();

    @BeforeEach
    void resetRegistry() {
        registry.setCustomCommands(List.of());
    }

    @AfterEach
    void restoreRegistry() {
        registry.setCustomCommands(List.of());
    }

    // --- getCustomCommands() ---

    @Test
    void getCustomCommandsReturnsEmptyListAfterReset() {
        assertTrue(registry.getCustomCommands().isEmpty());
    }

    @Test
    void setCustomCommandsReplacesExistingList() {
        CustomCommandDefinition customCommand = buildCustomCommand("custom_command_a", "alpha");
        registry.setCustomCommands(List.of(customCommand));
        assertEquals(1, registry.getCustomCommands().size());
        assertEquals("custom_command_a", registry.getCustomCommands().getFirst().getId());

        registry.setCustomCommands(List.of());
        assertTrue(registry.getCustomCommands().isEmpty());
    }

    @Test
    void replaceCustomCommandsUpdatesPublicSnapshot() {
        registry.replaceCustomCommands(List.of(buildCustomCommand("custom_command_replace", "replace phrase")));

        assertEquals(1, registry.getCustomCommands().size());
        assertEquals("custom_command_replace", registry.getCustomCommands().getFirst().getId());
    }

    @Test
    void getCustomCommandsListIsUnmodifiable() {
        registry.setCustomCommands(List.of(buildCustomCommand("custom_command_a", "alpha")));
        assertThrows(UnsupportedOperationException.class,
                () -> registry.getCustomCommands().add(null));
    }

    // --- contributeToActionMap() ---

    @Test
    void contributeToActionMapAddsAliasEntryForEachCustomCommandPhrase() {
        registry.setCustomCommands(List.of(
                buildCustomCommand("custom_command_a", "do alpha, alpha command"),
                buildCustomCommand("custom_command_b", "do beta")
        ));
        Map<String, String> map = new LinkedHashMap<>();
        registry.contributeToActionMap(map);

        assertEquals("custom_command_a", map.get("do alpha"));
        assertEquals("custom_command_a", map.get("alpha command"));
        assertEquals("custom_command_b", map.get("do beta"));
        assertEquals(3, map.size());
    }

    @Test
    void contributeToActionMapDoesNothingWhenEmpty() {
        Map<String, String> map = new LinkedHashMap<>();
        registry.contributeToActionMap(map);
        assertTrue(map.isEmpty());
    }

    @Test
    void contributeToActionMapDoesNotClearExistingEntries() {
        registry.setCustomCommands(List.of(buildCustomCommand("custom_command_x", "trigger x")));
        Map<String, String> map = new LinkedHashMap<>();
        map.put("existing_phrase", "existing_action");
        registry.contributeToActionMap(map);

        assertEquals("existing_action", map.get("existing_phrase"));
        assertEquals("custom_command_x", map.get("trigger x"));
    }

    @Test
    void contributeToActionMapDoesNotOverrideExistingPhrase() {
        registry.setCustomCommands(List.of(buildCustomCommand("custom_command_x", "existing_phrase, new phrase")));
        Map<String, String> map = new LinkedHashMap<>();
        map.put("existing_phrase", "existing_action");

        registry.contributeToActionMap(map);

        assertEquals("existing_action", map.get("existing_phrase"));
        assertEquals("custom_command_x", map.get("new phrase"));
        assertEquals(2, map.size());
    }

    @Test
    void reducerKeepsExactCustomCommandPhraseAsHighConfidenceCandidate() {
        registry.setCustomCommands(List.of(buildCustomCommand("custom_command_exact", "example custom command, custom command status check")));
        Map<String, String> map = new LinkedHashMap<>();
        registry.contributeToActionMap(map);

        Map<String, String> reduced = Reducer.reduce("custom command status check", map, false);

        assertEquals("custom_command_exact", reduced.get("custom command status check"));
    }

    @Test
    void contributeToActionMapNormalizesCustomCommandPhraseCaseForSttInput() {
        registry.setCustomCommands(List.of(buildCustomCommand("custom_command_case", "Example Custom Command")));
        Map<String, String> map = new LinkedHashMap<>();

        registry.contributeToActionMap(map);

        assertEquals("custom_command_case", map.get("example custom command"));
    }

    // --- contributeToHandlerMap() ---

    @Test
    void contributeToHandlerMapAddsCustomCommandHandlerPerCustomCommand() {
        registry.setCustomCommands(List.of(
                buildCustomCommand("custom_command_a", "phrase_a"),
                buildCustomCommand("custom_command_b", "phrase_b")
        ));
        Map<String, CommandHandler> handlers = new HashMap<>();
        registry.contributeToHandlerMap(handlers);

        assertTrue(handlers.containsKey("custom_command_a"));
        assertTrue(handlers.containsKey("custom_command_b"));
        assertInstanceOf(CustomCommandHandler.class, handlers.get("custom_command_a"));
        assertInstanceOf(CustomCommandHandler.class, handlers.get("custom_command_b"));
    }

    @Test
    void contributeToHandlerMapDoesNothingWhenEmpty() {
        Map<String, CommandHandler> handlers = new HashMap<>();
        registry.contributeToHandlerMap(handlers);
        assertTrue(handlers.isEmpty());
    }

    @Test
    void contributeToHandlerMapDoesNotOverrideExistingHandler() {
        registry.setCustomCommands(List.of(buildCustomCommand("existing_action", "phrase")));
        CommandHandler existingHandler = (action, params, responseText) -> null;
        Map<String, CommandHandler> handlers = new HashMap<>();
        handlers.put("existing_action", existingHandler);

        registry.contributeToHandlerMap(handlers);

        assertSame(existingHandler, handlers.get("existing_action"));
        assertEquals(1, handlers.size());
    }

    @Test
    void duplicateCustomCommandIdsLastOneWins() {
        CustomCommandDefinition first = buildCustomCommand("custom_command_dup", "phrase_first");
        CustomCommandDefinition second = buildCustomCommand("custom_command_dup", "phrase_second");
        registry.setCustomCommands(List.of(first, second));

        Map<String, CommandHandler> handlers = new HashMap<>();
        registry.contributeToHandlerMap(handlers);

        // Both were put; the map has one entry (last write wins in HashMap)
        assertEquals(1, handlers.size());
        assertTrue(handlers.containsKey("custom_command_dup"));
    }

    @Test
    void setNullCustomCommandsTreatedAsEmpty() {
        registry.setCustomCommands(null);
        assertTrue(registry.getCustomCommands().isEmpty());
    }

    // --- helpers ---

    private CustomCommandDefinition buildCustomCommand(String id, String phrases) {
        return GSON.fromJson(
                "{\"id\":\"" + id + "\",\"name\":\"" + id + "\",\"phrases\":\"" + phrases + "\"," +
                "\"steps\":[{\"type\":\"SPEAK\",\"text\":\"ok\"}]}",
                CustomCommandDefinition.class);
    }
}
