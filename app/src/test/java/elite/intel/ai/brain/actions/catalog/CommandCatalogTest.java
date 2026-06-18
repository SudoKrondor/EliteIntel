package elite.intel.ai.brain.actions.catalog;

import com.google.gson.Gson;
import elite.intel.ai.brain.actions.command.CommandRegistry;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.customcommand.CustomCommandDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CommandCatalogTest {

    private final CommandCatalog catalog = new CommandCatalog(Function.identity());

    @BeforeAll
    static void loadRegistry() {
        CommandRegistry.getInstance().load();
    }

    private static int registrySize() {
        return CommandRegistry.getInstance().byId().size();
    }

    @Test
    void containsOneEntryForEveryRegistryCommand() {
        Map<String, Long> entryCountsById = catalog.entries().stream()
                .collect(Collectors.groupingBy(CommandCatalogEntry::id, Collectors.counting()));
        Map<String, Long> registryCountsById = CommandRegistry.getInstance().byId().keySet().stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        assertEquals(registrySize(), catalog.entries().size());
        assertEquals(registryCountsById, entryCountsById);
    }

    @Test
    void everyEntryIdIsNonBlank() {
        for (CommandCatalogEntry entry : catalog.entries()) {
            assertFalse(entry.id().isBlank());
        }
    }

    @Test
    void entryTypeMatchesCommandKind() {
        Map<String, CommandCatalogEntry> entriesById = catalog.entries().stream()
                .collect(Collectors.toMap(CommandCatalogEntry::id, Function.identity()));

        for (IntelCommand command : CommandRegistry.getInstance().byId().values()) {
            CommandCatalogEntry entry = entriesById.get(command.id());
            assertNotNull(entry, command.id());
            assertEquals(expectedType(command), entry.type(), command.id());
        }
    }

    @Test
    void missingLocalizationKeysFallBackToNonBlankNameAndDescription() {
        // textResolver is Function.identity(): it echoes the i18n key back, simulating
        // missing translations, so every entry must use its non-blank fallbacks.
        for (CommandCatalogEntry entry : catalog.entries()) {
            assertFalse(entry.name().isBlank(), entry.id());
            assertFalse(entry.description().isBlank(), entry.id());
            assertNotEquals(localizationKey(entry.id(), "name"), entry.name(), entry.id());
            assertEquals("Built-in command action: " + entry.id(), entry.description(), entry.id());
        }
    }

    private CommandCatalogEntryType expectedType(IntelCommand command) {
        return switch (command.kind()) {
            case BINDING -> CommandCatalogEntryType.BUILT_IN_BINDING;
            case ACTION -> CommandCatalogEntryType.BUILT_IN_ACTION;
        };
    }

    private String localizationKey(String id, String field) {
        return "command." + id + "." + field;
    }

    // ---- entries(List<CustomCommandDefinition>) overload ----

    @Test
    void builtInEntriesStillPresentWhenCustomCommandListIsEmpty() {
        List<CommandCatalogEntry> entries = catalog.entries(List.of());
        assertEquals(registrySize(), entries.size());
    }

    @Test
    void customCommandEntriesAppendedAfterBuiltIns() {
        CustomCommandDefinition customCommand = buildCustomCommand("custom_command_test", "Test Custom Command", "desc");
        List<CommandCatalogEntry> entries = catalog.entries(List.of(customCommand));

        assertEquals(registrySize() + 1, entries.size());
        CommandCatalogEntry customCommandEntry = entries.getLast();
        assertEquals("custom_command_test", customCommandEntry.id());
        assertEquals("Test Custom Command", customCommandEntry.name());
    }

    @Test
    void customCommandEntryTypeIsUserCustomCommandAndIsCustomCommandReturnsTrue() {
        CustomCommandDefinition customCommand = buildCustomCommand("custom_command_x", "X", "desc");
        CommandCatalogEntry entry = catalog.entries(List.of(customCommand)).getLast();

        assertEquals(CommandCatalogEntryType.CUSTOM_COMMAND, entry.type());
        assertTrue(entry.isCustomCommand());
    }

    @Test
    void builtInEntriesHaveIsCustomCommandFalse() {
        for (CommandCatalogEntry entry : catalog.entries()) {
            assertFalse(entry.isCustomCommand(), "Built-in entry " + entry.id() + " must not be a customCommand");
        }
    }

    @Test
    void blankCustomCommandDescriptionFallsBackToDefaultText() {
        CustomCommandDefinition customCommand = buildCustomCommand("custom_command_nodesc", "My Custom Command", "");
        CommandCatalogEntry entry = catalog.entries(List.of(customCommand)).getLast();

        assertEquals("User custom command: My Custom Command", entry.description());
    }

    @Test
    void nonBlankCustomCommandDescriptionIsPreserved() {
        CustomCommandDefinition customCommand = buildCustomCommand("custom_command_desc", "My Custom Command", "Custom description");
        CommandCatalogEntry entry = catalog.entries(List.of(customCommand)).getLast();

        assertEquals("Custom description", entry.description());
    }

    @Test
    void distinctBindingIdsDeduplicatesAndExcludesNonBindingSteps() {
        CustomCommandDefinition customCommand = buildCustomCommand("custom_command_bindings", "Bindings", "desc",
                "[{\"type\":\"BINDING_TAP\",\"bindingId\":\"A\"}," +
                "{\"type\":\"BINDING_HOLD\",\"bindingId\":\"B\",\"durationMs\":200}," +
                "{\"type\":\"DELAY\",\"durationMs\":100}," +
                "{\"type\":\"SPEAK\",\"text\":\"hi\"}," +
                "{\"type\":\"BINDING_TAP\",\"bindingId\":\"A\"}]");

        List<String> ids = customCommand.distinctBindingIds();
        assertEquals(List.of("A", "B"), ids);
    }

    @Test
    void distinctBindingIdsEmptyWhenOnlyDelayAndSpeak() {
        CustomCommandDefinition customCommand = buildCustomCommand("custom_command_nodeps", "No Deps", "",
                "[{\"type\":\"DELAY\",\"durationMs\":0},{\"type\":\"SPEAK\",\"text\":\"x\"}]");

        assertTrue(customCommand.distinctBindingIds().isEmpty());
    }

    // ---- helpers for custom command tests ----

    private static final Gson CUSTOM_COMMAND_GSON = new Gson();

    private CustomCommandDefinition buildCustomCommand(String id, String name, String description) {
        return buildCustomCommand(id, name, description,
                "[{\"type\":\"SPEAK\",\"text\":\"ok\"}]");
    }

    private CustomCommandDefinition buildCustomCommand(String id, String name, String description, String stepsJson) {
        String json = "{\"id\":\"" + id + "\",\"name\":\"" + name + "\"," +
                      "\"description\":\"" + description + "\"," +
                      "\"phrases\":\"trigger " + id + "\"," +
                      "\"steps\":" + stepsJson + "}";
        return CUSTOM_COMMAND_GSON.fromJson(json, CustomCommandDefinition.class);
    }
}
