package elite.intel.ai.brain.actions.customcommand;

import elite.intel.ai.brain.InputNormalizer;
import elite.intel.ai.brain.actions.IntelAction;
import elite.intel.ai.brain.i18n.AiActionLocalizations;
import elite.intel.gameapi.EventBusManager;
import elite.intel.ui.event.CustomCommandsSummaryChangedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * A singleton registry for managing custom commands, providing the ability to load, retrieve,
 * replace, and interact with custom command definitions. This class manages runtime custom command
 * state and integrates them with various application components.
 *
 * <h2>Responsibilities</h2>
 * 1. Loading custom command definitions from a predefined source file.
 * 2. Returning immutable snapshots of the loaded custom commands.
 * 3. Maintaining and exposing the status of the most recent load operation, including skipped commands and backup restoration.
 * 4. Allowing integration of custom command data into the related runtime structures such as action maps, parameter rules, and handler maps.
 * 5. Supporting controlled modification of the in-memory custom commands without performing persistence operations.
 */
public final class CustomCommandRegistry {

    private static final Logger log = LogManager.getLogger(CustomCommandRegistry.class);
    private static final CustomCommandRegistry INSTANCE = new CustomCommandRegistry();

    /** Written once during {@link #load()}, read-only after. */
    private volatile List<CustomCommandDefinition> customCommands = Collections.emptyList();
    private final CustomCommandRepository repository = new CustomCommandRepository();

    private CustomCommandRegistry() {}

    public static CustomCommandRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Loads customCommands from {@code custom_commands.json}. Must be called before
     * {@code ResponseRouter} (and therefore {@code CommandHandlerFactory}) is first used.
     */
    public void load() {
        customCommands = repository.load();
        log.info("Custom command registry: {} command(s) loaded, {} skipped",
                customCommands.size(), repository.getLastSkippedCount());
    }

    /** Returns the loaded custom command list as an immutable snapshot. Empty until {@link #load()} is called. */
    public List<CustomCommandDefinition> getCustomCommands() {
        return List.copyOf(customCommands);
    }

    /** Returns the number of customCommands skipped during the most recent {@link #load()} due to validation failures. */
    public int getSkippedOnLastLoad() {
        return repository.getLastSkippedCount();
    }

    /** Returns human-readable labels for customCommands skipped during the most recent {@link #load()}. */
    public List<String> getSkippedLabelsOnLastLoad() {
        return repository.getLastSkippedLabels();
    }

    /** Returns {@code true} if the most recent {@link #load()} restored customCommands from the backup file. */
    public boolean wasLastLoadRestoredFromBackup() {
        return repository.wasRestoredFromBackup();
    }

    /**
     * Replaces the in-memory customCommand registry with a validated immutable snapshot.
     * Persistence is owned by {@link CustomCommandRepository}; this method only updates runtime state.
     */
    public void replaceCustomCommands(List<CustomCommandDefinition> customCommands) {
        setCustomCommands(customCommands);
        log.info("Custom command registry: {} command(s) active after replace", this.customCommands.size());
        EventBusManager.publish(new CustomCommandsSummaryChangedEvent(this.customCommands.size()));
    }

    /**
     * Adds custom command phrase->id entries to the LLM action map.
     * Called from {@code AiActionsMap.actionMap()} so custom command trigger phrases reach the Reducer
     * and appear in the ACTIONS block sent to the LLM.
     */
    public void contributeToActionMap(Map<String, String> map) {
        Set<String> protectedPhrases = new HashSet<>();
        for (String phrase : map.keySet()) {
            protectedPhrases.add(normalizePhrase(phrase));
        }
        for (CustomCommandDefinition customCommand : customCommands) {
            for (String phrase : AiActionLocalizations.splitPhraseGroup(customCommand.getPhrases())) {
                String normalizedPhrase = normalizePhrase(phrase);
                if (protectedPhrases.contains(normalizedPhrase)) {
                    // Graceful degradation: skip conflicting phrase rather than blocking the entire customCommand.
                    log.warn("CustomCommand phrase [{}] conflicts with an existing action map entry - phrase skipped", phrase);
                    continue;
                }
                map.put(normalizedPhrase, customCommand.getActionKey());
                protectedPhrases.add(normalizedPhrase);
                log.debug("CustomCommand phrase registered: [{}] -> {}", normalizedPhrase, customCommand.getActionKey());
            }
        }
    }

    private static String normalizePhrase(String phrase) {
        String normalized = InputNormalizer.getInstance().normalize(phrase == null ? "" : phrase);
        return normalized == null ? "" : normalized.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Package-private test seam - injects a known custom command list without file I/O.
     * Resets the registry to the provided list; {@link #load()} is not called.
     */
    void setCustomCommands(List<CustomCommandDefinition> customCommands) {
        this.customCommands = customCommands == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(customCommands));
    }

    /**
     * Appends the rules for custom command parameters to the provided {@code StringBuilder}.
     * This includes a description of required parameters for active custom commands
     * based on the given mapping of reduced actions.
     *
     * @param reducedActions A map associating string keys (e.g., action phrases) to their corresponding
     *                       action IDs for filtering relevant custom commands.
     * @param sb             A {@code StringBuilder} object to which the constructed rules for
     *                       custom command parameters will be appended.
     */
    public void appendCustomCommandParamRules(Map<String, String> reducedActions, StringBuilder sb) {
        Set<String> activeIds = new HashSet<>(reducedActions.values());
        List<CustomCommandDefinition> activeCustomCommands = customCommands.stream()
                .filter(m -> activeIds.contains(m.getActionKey()))
                .filter(m -> !m.getParameters().isEmpty())
                .toList();
        if (activeCustomCommands.isEmpty()) return;

        sb.append("""      
                CUSTOM COMMAND PARAMS (required for custom command actions above include ALL required params):
                
                """);
        for (CustomCommandDefinition customCommand : activeCustomCommands) {
            elite.intel.ai.brain.actions.command.CommandParamRules.appendCommandBlock(
                    customCommand.getActionKey(), customCommand.getParameters(), sb);
        }
    }

    /**
     * Registers a {@link CustomCommandHandler} per custom command into the command handler map.
     * Called from {@code CommandHandlerFactory.registerCommandHandlers()} so that
     * {@code ResponseRouter} routes user-defined action IDs through the normal command dispatch path.
     */
    public void contributeToHandlerMap(Map<String, IntelAction> map) {
        Set<String> protectedActionIds = new HashSet<>(map.keySet());
        for (CustomCommandDefinition customCommand : customCommands) {
            if (protectedActionIds.contains(customCommand.getActionKey())) {
                log.warn("Custom command actionKey '{}' conflicts with an existing command handler - custom command skipped",
                        customCommand.getActionKey());
                continue;
            }
            map.put(customCommand.getActionKey(), new CustomCommandHandler(customCommand));
            log.debug("Custom command handler registered: {}", customCommand.getActionKey());
        }
    }
}
