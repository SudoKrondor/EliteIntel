package elite.intel.ai.brain.actions.catalog;

import elite.intel.ai.brain.actions.command.CommandI18nKeys;
import elite.intel.ai.brain.actions.command.CommandKind;
import elite.intel.ai.brain.actions.command.CommandRegistry;
import elite.intel.ai.brain.actions.command.IntelCommand;
import elite.intel.ai.brain.actions.customcommand.CustomCommandDefinition;
import elite.intel.ui.i18n.MultiLingualTextProvider;
import elite.intel.util.StringUtls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Read-only projection of the built-in command registry.
 * <p>
 * The self-describing CommandRegistry is the source of truth: every catalog entry is
 * derived from CommandRegistry.byId(), and this class must not maintain a separate hardcoded
 * list of built-in commands. The catalog is metadata only; it does not execute
 * commands and is intentionally not wired into ResponseRouter or CommandHandlerFactory.
 */
public final class CommandCatalog {

    private final Function<String, String> textResolver;

    public CommandCatalog() {
        this(MultiLingualTextProvider::getText);
    }

    /**
     * Test seam for localization fallback behavior. Production code uses
     * {@link MultiLingualTextProvider} through the public constructor.
     */
    CommandCatalog(Function<String, String> textResolver) {
        this.textResolver = Objects.requireNonNull(textResolver, "textResolver");
    }

    public List<CommandCatalogEntry> entries() {
        // Source is the self-describing registry. byId() is a LinkedHashMap, but its
        // insertion order mirrors the Reflections scan and is NOT stable across JVM runs,
        // so we sort here (name, then id) — the same comparator the UI consumers already
        // apply — to keep entries() deterministic.
        return CommandRegistry.getInstance().byId().values().stream()
                .map(this::entryFrom)
                .sorted(Comparator.comparing(CommandCatalogEntry::name, String.CASE_INSENSITIVE_ORDER)
                                  .thenComparing(CommandCatalogEntry::id))
                .toList();
    }

    /**
     * Returns all catalog entries: built-in commands followed by user-defined customCommands.
     * Built-in entries are derived from CommandRegistry.byId() as before; custom command entries
     * are built from the provided list. The existing {@link #entries()} method is unchanged.
     */
    public List<CommandCatalogEntry> entries(List<CustomCommandDefinition> customCommands) {
        Objects.requireNonNull(customCommands, "customCommands");
        List<CommandCatalogEntry> all = new ArrayList<>(entries());
        for (CustomCommandDefinition customCommand : customCommands) {
            String desc = customCommand.getDescription().isBlank()
                    ? "User custom command: " + customCommand.getName()
                    : customCommand.getDescription();
            all.add(new CommandCatalogEntry(customCommand.getActionKey(), customCommand.getName(), desc, CommandCatalogEntryType.CUSTOM_COMMAND));
        }
        return Collections.unmodifiableList(all);
    }

    public Optional<CommandCatalogEntry> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return entries().stream()
                .filter(entry -> entry.id().equalsIgnoreCase(id))
                .findFirst();
    }

    private CommandCatalogEntry entryFrom(IntelCommand command) {
        Objects.requireNonNull(command, "command");
        String id = command.id();
        return new CommandCatalogEntry(
                id,
                localizedName(id),
                localizedDescription(id),
                kindToType(command.kind())
        );
    }

    /** Maps the command's self-described {@link CommandKind} to the catalog's display type. */
    private static CommandCatalogEntryType kindToType(CommandKind kind) {
        return switch (kind) {
            case BINDING -> CommandCatalogEntryType.BUILT_IN_BINDING;
            case ACTION -> CommandCatalogEntryType.BUILT_IN_ACTION;
        };
    }

    private String localizedName(String id) {
        String key = CommandI18nKeys.nameKey(id);
        String localized = textResolver.apply(key);
        if (!key.equals(localized)) {
            return localized;
        }
        return humanize(id);
    }

    private String localizedDescription(String id) {
        String key = CommandI18nKeys.descriptionKey(id);
        String localized = textResolver.apply(key);
        if (!key.equals(localized)) {
            return localized;
        }
        return "Built-in command action: " + id;
    }

    private static String humanize(String value) {
        String text = value == null || value.isBlank() ? "Command" : value;
        String humanized = StringUtls.capitalizeWords(text.replace('_', ' '));
        return humanized == null || humanized.isBlank() ? "Command" : humanized;
    }
}
